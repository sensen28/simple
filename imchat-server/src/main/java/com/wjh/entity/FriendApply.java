package com.wjh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友申请实体类
 */
@Data
@TableName("friend_apply")
public class FriendApply {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 申请人ID
     */
    private Long applyUserId;

    /**
     * 目标用户ID
     */
    private Long targetUserId;

    /**
     * 申请备注
     */
    private String remark;

    /**
     * 申请状态：0-待处理，1-同意，2-拒绝
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
