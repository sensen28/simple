package com.wjh.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 好友申请DTO
 */
@Data
public class FriendApplyDTO {

    /**
     * 目标用户ID
     */
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;

    /**
     * 申请备注
     */
    private String remark;
}
