package com.wjh.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjh.common.domain.dto.MessageSendDTO;
import com.wjh.common.enums.MessageTypeEnum;
import com.wjh.common.protocol.MessageProtocol;
import com.wjh.common.utils.JwtUtils;
import com.wjh.entity.Message;
import com.wjh.entity.User;
import com.wjh.netty.ChannelManager;
import com.wjh.service.FriendService;
import com.wjh.service.MessageService;
import com.wjh.service.UserService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务消息处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MessageHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private MessageService messageService;

    @Resource
    private FriendService friendService;

    @Resource
    private UserService userService;

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
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        Map<String, String> map = objectMapper.readValue(body, Map.class);
        String token = map.get("token");
        // 验证token
        if (token != null && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            // 绑定用户和通道
            ChannelManager.bindUser(userId, ctx.channel());
            // 更新在线状态
            userService.lambdaUpdate()
                    .set(User::getStatus, 1)
                    .set(User::getUpdateTime, LocalDateTime.now())
                    .eq(User::getId, userId)
                    .update();
            // 返回认证成功响应
            MessageProtocol response = new MessageProtocol();
            response.setMessageType(MessageTypeEnum.AUTH_RESPONSE.getCode().byteValue());
            response.setStatus((byte) 0);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("code", 200);
            result.put("msg", "认证成功");
            result.put("userId", userId);
            result.put("username", username);
            byte[] data = objectMapper.writeValueAsBytes(result);
            response.setLength(data.length);
            response.setBody(data);
            ctx.writeAndFlush(response);
            // 推送离线消息
            sendOfflineMessages(ctx, userId);
            log.info("用户{}认证成功", userId);
        } else {
            // 认证失败
            MessageProtocol response = new MessageProtocol();
            response.setMessageType(MessageTypeEnum.AUTH_RESPONSE.getCode().byteValue());
            response.setStatus((byte) 1);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("code", 401);
            result.put("msg", "token无效或已过期");
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
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        Long toUserId = parseLong(map.get("toUserId"));
        String content = map.get("content") == null ? "" : map.get("content").toString();
        Integer type = parseInt(map.get("type"), 1);
        if (toUserId == null) {
            sendAck(ctx, null, 400, "接收用户ID不能为空");
            return;
        }
        if (!StringUtils.hasText(content)) {
            sendAck(ctx, null, 400, "消息内容不能为空");
            return;
        }
        if (!friendService.isFriend(userId, toUserId)) {
            sendAck(ctx, null, 400, "对方不是你的好友，无法发送消息");
            return;
        }

        MessageSendDTO sendDTO = new MessageSendDTO();
        sendDTO.setToUserId(toUserId);
        sendDTO.setContent(content);
        sendDTO.setType(type);
        Message message = messageService.savePrivateMessage(userId, sendDTO);

        MessageProtocol forwardMsg = new MessageProtocol();
        forwardMsg.setMessageType(MessageTypeEnum.PRIVATE_MESSAGE.getCode().byteValue());
        forwardMsg.setStatus((byte) 0);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("msgId", message.getId());
        result.put("fromUserId", userId);
        result.put("toUserId", toUserId);
        result.put("content", content);
        result.put("type", type);
        result.put("timestamp", System.currentTimeMillis());
        byte[] data = objectMapper.writeValueAsBytes(result);
        forwardMsg.setLength(data.length);
        forwardMsg.setBody(data);
        // 检查用户是否在线
        if (ChannelManager.isOnline(toUserId)) {
            ChannelManager.sendToUser(toUserId, forwardMsg);
            log.info("消息实时转发成功，toUserId={}", toUserId);
        } else {
            log.info("用户{}不在线，消息已存入离线队列", toUserId);
        }
        // 返回ACK给发送者
        sendAck(ctx, message.getId(), 200, "发送成功");
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
        Long userId = ChannelManager.getUserIdByChannel(ctx.channel());
        if (userId == null) {
            return;
        }
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        Long msgId = parseLong(map.get("msgId"));
        if (msgId == null) {
            return;
        }
        // 更新消息状态为已读
        messageService.markReadByMsgId(userId, msgId);
        log.info("消息{}已被用户{}确认", msgId, userId);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelManager.addChannel(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long userId = ChannelManager.getUserIdByChannel(ctx.channel());
        ChannelManager.removeChannel(ctx.channel());
        if (userId != null) {
            userService.lambdaUpdate()
                    .set(User::getStatus, 0)
                    .set(User::getUpdateTime, LocalDateTime.now())
                    .eq(User::getId, userId)
                    .update();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("消息处理异常: {}", cause.getMessage(), cause);
        ChannelManager.removeChannel(ctx.channel());
        ctx.close();
    }

    private void sendOfflineMessages(ChannelHandlerContext ctx, Long userId) throws Exception {
        List<Message> offlineMessages = messageService.listOfflineMessages(userId, 200);
        if (offlineMessages.isEmpty()) {
            return;
        }
        for (Message message : offlineMessages) {
            MessageProtocol protocol = new MessageProtocol();
            protocol.setMessageType(MessageTypeEnum.PRIVATE_MESSAGE.getCode().byteValue());
            protocol.setStatus((byte) 0);
            Map<String, Object> payload = new HashMap<>();
            payload.put("msgId", message.getId());
            payload.put("fromUserId", message.getFromUserId());
            payload.put("toUserId", message.getToUserId());
            payload.put("content", message.getContent());
            payload.put("type", message.getType());
            payload.put("timestamp", message.getCreateTime() == null
                    ? System.currentTimeMillis()
                    : message.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            payload.put("offline", true);
            byte[] body = objectMapper.writeValueAsBytes(payload);
            protocol.setLength(body.length);
            protocol.setBody(body);
            ctx.writeAndFlush(protocol);
        }
    }

    private void sendAck(ChannelHandlerContext ctx, Long msgId, Integer code, String msg) throws Exception {
        MessageProtocol ack = new MessageProtocol();
        ack.setMessageType(MessageTypeEnum.MESSAGE_ACK.getCode().byteValue());
        ack.setStatus((byte) (code != null && code == 200 ? 0 : 1));
        Map<String, Object> ackResult = new HashMap<>();
        ackResult.put("msgId", msgId);
        ackResult.put("code", code);
        ackResult.put("msg", msg);
        byte[] ackData = objectMapper.writeValueAsBytes(ackResult);
        ack.setLength(ackData.length);
        ack.setBody(ackData);
        ctx.writeAndFlush(ack);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
