package com.wjh.common.enums;

import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
public enum MessageTypeEnum {

    /**
     * 认证请求
     */
    AUTH_REQUEST(1, "认证请求"),

    /**
     * 认证响应
     */
    AUTH_RESPONSE(2, "认证响应"),

    /**
     * 心跳请求
     */
    HEARTBEAT_REQUEST(3, "心跳请求"),

    /**
     * 心跳响应
     */
    HEARTBEAT_RESPONSE(4, "心跳响应"),

    /**
     * 单聊消息
     */
    PRIVATE_MESSAGE(5, "单聊消息"),

    /**
     * 群聊消息
     */
    GROUP_MESSAGE(6, "群聊消息"),

    /**
     * 消息ACK
     */
    MESSAGE_ACK(7, "消息ACK"),

    /**
     * 好友申请通知
     */
    FRIEND_APPLY_NOTICE(8, "好友申请通知"),

    /**
     * 好友状态变更通知
     */
    FRIEND_STATUS_NOTICE(9, "好友状态变更通知"),

    /**
     * 系统通知
     */
    SYSTEM_NOTICE(10, "系统通知");

    private final Integer code;
    private final String desc;

    MessageTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MessageTypeEnum getByCode(Integer code) {
        for (MessageTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
