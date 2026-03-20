package com.wjh.netty.handler;

import com.wjh.config.NettyConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * WebSocket握手处理器
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    @Resource
    private NettyConfig nettyConfig;

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest request) {
            // 处理HTTP握手请求
            handleHttpRequest(ctx, request);
        } else if (msg instanceof WebSocketFrame frame) {
            // 处理WebSocket帧
            handleWebSocketFrame(ctx, frame);
        }
    }

    /**
     * 处理HTTP请求，完成WebSocket握手
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 校验HTTP请求
        if (!request.decoderResult().isSuccess()
                || !"websocket".equals(request.headers().get("Upgrade"))) {
            ctx.close();
            return;
        }
        // 构造握手响应
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(request), null, true, 65536 * 10);
        handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), request);
            log.info("WebSocket握手成功: {}", ctx.channel().id());
        }
    }

    /**
     * 处理WebSocket帧
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 关闭帧
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // Ping帧
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // Pong帧
        if (frame instanceof PongWebSocketFrame) {
            return;
        }
        // 二进制消息，传递给下一个处理器
        if (frame instanceof BinaryWebSocketFrame binaryFrame) {
            ctx.fireChannelRead(binaryFrame.content().retain());
        }
    }

    /**
     * 获取WebSocket地址
     */
    private String getWebSocketLocation(FullHttpRequest request) {
        String location = request.headers().get("Host") + nettyConfig.getWebsocketPath();
        return "ws://" + location;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket处理异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
