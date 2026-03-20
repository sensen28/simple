package com.wjh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wjh.common.domain.dto.LoginDTO;
import com.wjh.common.domain.dto.RegisterDTO;
import com.wjh.common.domain.vo.LoginVO;
import com.wjh.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param registerDTO 注册参数
     * @return 是否成功
     */
    boolean register(RegisterDTO registerDTO);

    /**
     * 用户登录
     * @param loginDTO 登录参数
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
}
