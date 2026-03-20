package com.wjh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友关系实体类
 */
@Data
@TableName("friend")
public class Friend {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 好友ID
     */
    private Long friendId;

    /**
     * 好友备注
     */
    private String remark;

    /**
     * 关系状态：0-拉黑，1-正常
     */
    private Integer status;

    /**
     * 是否免打扰：0-提醒，1-免打扰
     */
    private Integer isMute;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
