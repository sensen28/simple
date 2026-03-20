package com.wjh.controller;

import com.wjh.common.domain.Result;
import com.wjh.common.domain.vo.UserSearchVO;
import com.wjh.service.UserService;
import com.wjh.utils.SecurityUtils;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 搜索用户（用于添加好友）
     */
    @GetMapping("/search")
    public Result<List<UserSearchVO>> searchUsers(@RequestParam String keyword) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSearchVO> users = userService.searchUsers(keyword, currentUserId);
        return Result.success(users);
    }
}
