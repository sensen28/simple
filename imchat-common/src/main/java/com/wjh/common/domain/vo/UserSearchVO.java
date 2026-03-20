package com.wjh.common.domain.vo;

import lombok.Data;

/**
 * 用户搜索结果VO
 */
@Data
public class UserSearchVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 签名
     */
    private String signature;

    /**
     * 在线状态
     */
    private Integer status;
}
