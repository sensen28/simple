package com.wjh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息实体类
 */
@Data
@TableName("message")
public class Message {

    /**
     * 消息ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 消息类型：1-文本，2-图片，3-文件，4-语音，5-视频
     */
    private Integer type;

    /**
     * 消息状态：0-未读，1-已读，2-撤回
     */
    private Integer status;

    /**
     * 是否加密：0-未加密，1-已加密
     */
    private Integer isEncrypted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
