package com.wjh.common.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话摘要VO
 */
@Data
public class ConversationVO {

    /**
     * 会话对方用户ID
     */
    private Long friendId;

    /**
     * 对方用户名
     */
    private String friendUsername;

    /**
     * 对方昵称
     */
    private String friendNickname;

    /**
     * 对方头像
     */
    private String friendAvatar;

    /**
     * 最后一条消息
     */
    private String lastContent;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastTime;

    /**
     * 未读数量
     */
    private Long unreadCount;
}
