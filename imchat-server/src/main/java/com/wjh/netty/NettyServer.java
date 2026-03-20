package com.wjh.netty;

import com.wjh.config.NettyConfig;
import com.wjh.netty.codec.MessageCodec;
import com.wjh.netty.handler.HeartbeatHandler;
import com.wjh.netty.handler.MessageHandler;
import com.wjh.netty.handler.WebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket服务器
 */
@Slf4j
@Component
public class NettyServer {

    @Resource
    private NettyConfig nettyConfig;

    @Resource
    private ApplicationContext applicationContext;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    /**
     * 启动Netty服务器
     */
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(nettyConfig.getBossThreads());
        workerGroup = new NioEventLoopGroup(nettyConfig.getWorkerThreads());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // HTTP编解码器
                            ch.pipeline().addLast(new HttpServerCodec());
                            // 聚合HTTP请求
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            // 支持异步发送大的码流
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            // 聚合WebSocket帧
                            ch.pipeline().addLast(new WebSocketFrameAggregator(65536 * 10));
                            // WebSocket握手处理器，从Spring上下文获取新实例
                            ch.pipeline().addLast(applicationContext.getBean(WebSocketHandler.class));
                            // 心跳检测
                            ch.pipeline().addLast(new IdleStateHandler(
                                    nettyConfig.getHeartbeatTimeout(), 0, 0, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new HeartbeatHandler());
                            // 自定义消息编解码器
                            ch.pipeline().addLast(new MessageCodec());
                            // 业务消息处理器，从Spring上下文获取新实例
                            ch.pipeline().addLast(applicationContext.getBean(MessageHandler.class));
                        }
                    });
            // 绑定端口，同步等待成功
            bootstrap.bind(nettyConfig.getPort()).sync();
            log.info("Netty WebSocket服务器启动成功，监听端口: {}", nettyConfig.getPort());
        } catch (Exception e) {
            log.error("Netty WebSocket服务器启动失败", e);
            stop();
        }
    }

    /**
     * 关闭Netty服务器
     */
    @PreDestroy
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty WebSocket服务器已关闭");
    }
}
