package com.wjh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wjh.common.domain.dto.LoginDTO;
import com.wjh.common.domain.dto.RegisterDTO;
import com.wjh.common.domain.vo.LoginVO;
import com.wjh.common.domain.vo.UserSearchVO;
import com.wjh.common.utils.JwtUtils;
import com.wjh.entity.User;
import com.wjh.mapper.UserMapper;
import com.wjh.service.UserService;
import javax.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtils jwtUtils;

    @Override
    public boolean register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        User existUser = getUserByUsername(registerDTO.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getUsername());
        user.setStatus(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return save(user);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 查询用户
        User user = getUserByUsername(loginDTO.getUsername());
        if (user == null) {
            return null;
        }
        // 校验密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return null;
        }
        // 生成token
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        // 构造返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        // 更新用户状态为在线
        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        return loginVO;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return getOne(queryWrapper);
    }

    @Override
    public List<UserSearchVO> searchUsers(String keyword, Long excludeUserId) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
                .last("limit 20");
        List<User> users = list(queryWrapper);
        return users.stream()
                .filter(user -> excludeUserId == null || !excludeUserId.equals(user.getId()))
                .map(user -> {
                    UserSearchVO vo = new UserSearchVO();
                    vo.setUserId(user.getId());
                    vo.setUsername(user.getUsername());
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                    vo.setSignature(user.getSignature());
                    vo.setStatus(user.getStatus());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
