package com.wjh.common.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请展示VO
 */
@Data
public class FriendApplyVO {

    /**
     * 申请记录ID
     */
    private Long applyId;

    /**
     * 申请人ID
     */
    private Long applyUserId;

    /**
     * 申请人用户名
     */
    private String applyUsername;

    /**
     * 申请人昵称
     */
    private String applyNickname;

    /**
     * 申请人头像
     */
    private String applyAvatar;

    /**
     * 申请备注
     */
    private String remark;

    /**
     * 状态：0-待处理，1-同意，2-拒绝
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
