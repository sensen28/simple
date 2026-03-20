package com.wjh.netty.handler;

import com.wjh.common.enums.MessageTypeEnum;
import com.wjh.common.protocol.MessageProtocol;
import com.wjh.netty.ChannelManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳处理器
 */
@Slf4j
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    /**
     * 最大心跳丢失次数
     */
    private static final int MAX_LOSS_COUNT = 3;

    /**
     * 心跳丢失计数
     */
    private int lossCount = 0;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                // 读空闲，心跳丢失
                lossCount++;
                if (lossCount > MAX_LOSS_COUNT) {
                    log.warn("心跳超时，关闭连接: {}", ctx.channel().id());
                    ChannelManager.removeChannel(ctx.channel());
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof MessageProtocol message) {
            if (message.getMessageType().equals(MessageTypeEnum.HEARTBEAT_REQUEST.getCode().byteValue())) {
                // 心跳请求，响应心跳
                MessageProtocol response = new MessageProtocol();
                response.setMessageType(MessageTypeEnum.HEARTBEAT_RESPONSE.getCode().byteValue());
                response.setStatus((byte) 0);
                response.setLength(0);
                response.setBody(new byte[0]);
                ctx.writeAndFlush(response);
                lossCount = 0;
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("心跳处理异常: {}", cause.getMessage(), cause);
        ChannelManager.removeChannel(ctx.channel());
        ctx.close();
    }
}
