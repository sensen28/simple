package com.wjh.common.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息VO
 */
@Data
public class MessageVO {

    /**
     * 消息ID
     */
    private Long msgId;

    /**
     * 发送者ID
     */
    private Long fromUserId;

    /**
     * 接收者ID
     */
    private Long toUserId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息状态：0-未读，1-已读，2-撤回
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
