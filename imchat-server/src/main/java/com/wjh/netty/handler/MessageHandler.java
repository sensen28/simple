package com.wjh.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjh.common.enums.MessageTypeEnum;
import com.wjh.common.protocol.MessageProtocol;
import com.wjh.common.utils.JwtUtils;
import com.wjh.entity.Message;
import com.wjh.netty.ChannelManager;
import com.wjh.service.MessageService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 业务消息处理器
 */
@Slf4j
@Component
public class MessageHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private MessageService messageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        MessageTypeEnum messageType = MessageTypeEnum.getByCode((int) msg.getMessageType());
        if (messageType == null) {
            log.error("未知的消息类型: {}", msg.getMessageType());
            return;
        }
        switch (messageType) {
            case AUTH_REQUEST:
                handleAuthRequest(ctx, msg);
                break;
            case PRIVATE_MESSAGE:
                handlePrivateMessage(ctx, msg);
                break;
            case GROUP_MESSAGE:
                handleGroupMessage(ctx, msg);
                break;
            case MESSAGE_ACK:
                handleMessageAck(ctx, msg);
                break;
            default:
                log.warn("未处理的消息类型: {}", messageType);
        }
    }

    /**
     * 处理认证请求
     */
    private void handleAuthRequest(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        String body = new String(msg.getBody());
        Map<String, String> map = objectMapper.readValue(body, Map.class);
        String token = map.get("token");
        // 验证token
        if (token != null && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            // 绑定用户和通道
            ChannelManager.bindUser(userId, ctx.channel());
            // 返回认证成功响应
            MessageProtocol response = new MessageProtocol();
            response.setMessageType(MessageTypeEnum.AUTH_RESPONSE.getCode().byteValue());
            response.setStatus((byte) 0);
            Map<String, Object> result = Map.of(
                    "code", 200,
                    "msg", "认证成功",
                    "userId", userId,
                    "username", username
            );
            byte[] data = objectMapper.writeValueAsBytes(result);
            response.setLength(data.length);
            response.setBody(data);
            ctx.writeAndFlush(response);
            log.info("用户{}认证成功", userId);
        } else {
            // 认证失败
            MessageProtocol response = new MessageProtocol();
            response.setMessageType(MessageTypeEnum.AUTH_RESPONSE.getCode().byteValue());
            response.setStatus((byte) 1);
            Map<String, Object> result = Map.of(
                    "code", 401,
                    "msg", "token无效或已过期"
            );
            byte[] data = objectMapper.writeValueAsBytes(result);
            response.setLength(data.length);
            response.setBody(data);
            ctx.writeAndFlush(response);
            ctx.close();
            log.warn("认证失败，关闭连接: {}", ctx.channel().id());
        }
    }

    /**
     * 处理单聊消息
     */
    private void handlePrivateMessage(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        Long userId = ChannelManager.getUserIdByChannel(ctx.channel());
        if (userId == null) {
            log.warn("用户未认证，无法发送消息: {}", ctx.channel().id());
            return;
        }
        String body = new String(msg.getBody());
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        Long toUserId = Long.valueOf(map.get("toUserId").toString());
        String content = map.get("content").toString();
        Integer type = (Integer) map.getOrDefault("type", 1);
        // 保存消息到数据库
        Message message = new Message();
        message.setFromUserId(userId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setType(type);
        message.setStatus(0);
        message.setIsEncrypted(1);
        message.setCreateTime(LocalDateTime.now());
        messageService.save(message);
        // 转发给目标用户
        MessageProtocol forwardMsg = new MessageProtocol();
        forwardMsg.setMessageType(MessageTypeEnum.PRIVATE_MESSAGE.getCode().byteValue());
        forwardMsg.setStatus((byte) 0);
        Map<String, Object> result = Map.of(
                "msgId", message.getId(),
                "fromUserId", userId,
                "toUserId", toUserId,
                "content", content,
                "type", type,
                "timestamp", System.currentTimeMillis()
        );
        byte[] data = objectMapper.writeValueAsBytes(result);
        forwardMsg.setLength(data.length);
        forwardMsg.setBody(data);
        // 检查用户是否在线
        if (ChannelManager.isOnline(toUserId)) {
            ChannelManager.sendToUser(toUserId, forwardMsg);
            // 更新消息状态为已发送
            message.setStatus(1);
            messageService.updateById(message);
            log.info("消息已转发给用户{}", toUserId);
        } else {
            log.info("用户{}不在线，消息已存入离线队列", toUserId);
        }
        // 返回ACK给发送者
        MessageProtocol ack = new MessageProtocol();
        ack.setMessageType(MessageTypeEnum.MESSAGE_ACK.getCode().byteValue());
        ack.setStatus((byte) 0);
        Map<String, Object> ackResult = Map.of(
                "msgId", message.getId(),
                "status", 1
        );
        byte[] ackData = objectMapper.writeValueAsBytes(ackResult);
        ack.setLength(ackData.length);
        ack.setBody(ackData);
        ctx.writeAndFlush(ack);
    }

    /**
     * 处理群聊消息
     */
    private void handleGroupMessage(ChannelHandlerContext ctx, MessageProtocol msg) {
        // TODO 实现群聊消息逻辑
        log.info("收到群聊消息，待实现");
    }

    /**
     * 处理消息ACK
     */
    private void handleMessageAck(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        String body = new String(msg.getBody());
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        Long msgId = Long.valueOf(map.get("msgId").toString());
        // 更新消息状态为已读
        messageService.lambdaUpdate()
                .set(Message::getStatus, 1)
                .eq(Message::getId, msgId)
                .update();
        log.info("消息{}已确认", msgId);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelManager.addChannel(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ChannelManager.removeChannel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("消息处理异常: {}", cause.getMessage(), cause);
        ChannelManager.removeChannel(ctx.channel());
        ctx.close();
    }
}
