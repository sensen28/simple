package com.wjh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Netty配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "netty")
public class NettyConfig {

    /**
     * 监听端口
     */
    private Integer port = 9000;

    /**
     * WebSocket路径
     */
    private String websocketPath = "/ws";

    /**
     * Boss线程数
     */
    private Integer bossThreads = 1;

    /**
     * Worker线程数
     */
    private Integer workerThreads = 4;

    /**
     * 心跳超时时间（秒）
     */
    private Integer heartbeatTimeout = 60;
}
