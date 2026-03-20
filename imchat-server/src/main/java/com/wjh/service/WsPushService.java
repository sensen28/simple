package com.wjh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjh.common.enums.MessageTypeEnum;
import com.wjh.common.protocol.MessageProtocol;
import com.wjh.netty.ChannelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket推送服务
 */
@Slf4j
@Component
public class WsPushService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 推送消息给指定用户
     */
    public void push(Long userId, MessageTypeEnum messageType, Map<String, Object> payload) {
        if (userId == null || messageType == null || !ChannelManager.isOnline(userId)) {
            return;
        }
        try {
            MessageProtocol protocol = new MessageProtocol();
            protocol.setMessageType(messageType.getCode().byteValue());
            protocol.setStatus((byte) 0);
            byte[] body = objectMapper.writeValueAsBytes(payload);
            protocol.setLength(body.length);
            protocol.setBody(body);
            ChannelManager.sendToUser(userId, protocol);
        } catch (Exception e) {
            log.warn("推送消息失败，userId={}, type={}, reason={}", userId, messageType, e.getMessage());
        }
    }
}
