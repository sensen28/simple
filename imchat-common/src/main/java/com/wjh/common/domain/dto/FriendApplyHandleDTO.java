package com.wjh.common.domain.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理好友申请DTO
 */
@Data
public class FriendApplyHandleDTO {

    /**
     * 申请ID
     */
    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    /**
     * 操作类型：1-同意，2-拒绝
     */
    @NotNull(message = "操作类型不能为空")
    private Integer operateType;
}
