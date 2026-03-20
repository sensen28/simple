package com.wjh.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通道管理器，管理在线用户的连接
 */
@Slf4j
public class ChannelManager {

    /**
     * 所有连接的通道
     */
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 用户ID和Channel的映射
     */
    private static final Map<Long, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * ChannelId和用户ID的映射
     */
    private static final Map<ChannelId, Long> CHANNEL_USER_MAP = new ConcurrentHashMap<>();

    /**
     * 添加通道
     */
    public static void addChannel(Channel channel) {
        CHANNELS.add(channel);
        log.info("新连接加入: {}, 当前在线人数: {}", channel.id(), CHANNELS.size());
    }

    /**
     * 移除通道
     */
    public static void removeChannel(Channel channel) {
        CHANNELS.remove(channel);
        Long userId = CHANNEL_USER_MAP.remove(channel.id());
        if (userId != null) {
            USER_CHANNEL_MAP.remove(userId);
            log.info("用户{}断开连接, 当前在线人数: {}", userId, CHANNELS.size());
        } else {
            log.info("未认证连接断开: {}, 当前在线人数: {}", channel.id(), CHANNELS.size());
        }
    }

    /**
     * 用户认证成功，绑定用户和通道
     */
    public static void bindUser(Long userId, Channel channel) {
        USER_CHANNEL_MAP.put(userId, channel);
        CHANNEL_USER_MAP.put(channel.id(), userId);
        log.info("用户{}绑定通道成功", userId);
    }

    /**
     * 根据用户ID获取通道
     */
    public static Channel getChannelByUserId(Long userId) {
        return USER_CHANNEL_MAP.get(userId);
    }

    /**
     * 根据通道ID获取用户ID
     */
    public static Long getUserIdByChannel(Channel channel) {
        return CHANNEL_USER_MAP.get(channel.id());
    }

    /**
     * 判断用户是否在线
     */
    public static boolean isOnline(Long userId) {
        return USER_CHANNEL_MAP.containsKey(userId);
    }

    /**
     * 发送消息给指定用户
     */
    public static void sendToUser(Long userId, Object message) {
        Channel channel = getChannelByUserId(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public static void broadcast(Object message) {
        CHANNELS.writeAndFlush(message);
    }

    /**
     * 获取在线用户数量
     */
    public static int getOnlineCount() {
        return USER_CHANNEL_MAP.size();
    }
}
