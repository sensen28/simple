package com.wjh.common.domain.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息DTO
 */
@Data
public class MessageSendDTO {

    /**
     * 接收用户ID
     */
    @NotNull(message = "接收用户ID不能为空")
    private Long toUserId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 消息类型：1-文本，2-图片，3-文件，4-语音，5-视频
     */
    private Integer type = 1;
}
