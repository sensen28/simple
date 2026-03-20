package com.wjh.controller;

import cn.hutool.core.util.IdUtil;
import com.wjh.common.domain.Result;
import com.wjh.common.domain.dto.LoginDTO;
import com.wjh.common.domain.dto.RegisterDTO;
import com.wjh.common.domain.vo.LoginVO;
import com.wjh.common.utils.CaptchaUtils;
import com.wjh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private CaptchaUtils captchaUtils;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${imchat.captcha-expire:5}")
    private Integer captchaExpire;

    /**
     * 获取图片验证码
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        // 生成验证码
        CaptchaUtils.CaptchaInfo captchaInfo = captchaUtils.generateCaptcha();
        // 生成验证码key
        String captchaKey = IdUtil.simpleUUID();
        // 存入Redis，有效期5分钟
        stringRedisTemplate.opsForValue().set("captcha:" + captchaKey, captchaInfo.getCode(), captchaExpire, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", captchaInfo.getImageBase64());
        return Result.success(result);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterDTO registerDTO) {
        // 校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get("captcha:" + registerDTO.getCaptchaKey());
        if (cacheCode == null || !cacheCode.equalsIgnoreCase(registerDTO.getCaptcha())) {
            return Result.error("验证码错误或已过期");
        }
        // 删除验证码
        stringRedisTemplate.delete("captcha:" + registerDTO.getCaptchaKey());
        // 校验两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return Result.error("两次密码输入不一致");
        }
        // 注册
        boolean success = userService.register(registerDTO);
        return success ? Result.success("注册成功", true) : Result.error("注册失败");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        // 校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get("captcha:" + loginDTO.getCaptchaKey());
        if (cacheCode == null || !cacheCode.equalsIgnoreCase(loginDTO.getCaptcha())) {
            return Result.error("验证码错误或已过期");
        }
        // 删除验证码
        stringRedisTemplate.delete("captcha:" + loginDTO.getCaptchaKey());
        // 登录
        LoginVO loginVO = userService.login(loginDTO);
        return loginVO != null ? Result.success("登录成功", loginVO) : Result.error("用户名或密码错误");
    }
}
