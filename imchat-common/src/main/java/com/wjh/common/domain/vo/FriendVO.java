package com.wjh.common.domain.vo;

import lombok.Data;

/**
 * 好友信息VO
 */
@Data
public class FriendVO {

    /**
     * 好友ID
     */
    private Long friendId;

    /**
     * 好友用户名
     */
    private String username;

    /**
     * 好友昵称
     */
    private String nickname;

    /**
     * 好友头像
     */
    private String avatar;

    /**
     * 好友备注
     */
    private String remark;

    /**
     * 在线状态：0-离线，1-在线，2-隐身
     */
    private Integer status;

    /**
     * 是否免打扰：0-提醒，1-免打扰
     */
    private Integer isMute;
}
