package com.wjh.common.utils;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 验证码工具类
 */
@Component
public class CaptchaUtils {

    /**
     * 生成图片验证码
     * @param width 图片宽度
     * @param height 图片高度
     * @param codeCount 验证码位数
     * @param circleCount 干扰圈数量
     * @return 验证码对象
     */
    public CaptchaInfo generateCaptcha(int width, int height, int codeCount, int circleCount) {
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(width, height, codeCount, circleCount);
        String code = captcha.getCode();
        String imageBase64 = captcha.getImageBase64Data();

        CaptchaInfo captchaInfo = new CaptchaInfo();
        captchaInfo.setCode(code);
        captchaInfo.setImageBase64(imageBase64);
        return captchaInfo;
    }

    /**
     * 生成默认配置的验证码
     * @return 验证码对象
     */
    public CaptchaInfo generateCaptcha() {
        return generateCaptcha(120, 40, 4, 20);
    }

    @Data
    public static class CaptchaInfo {
        /**
         * 验证码文本
         */
        private String code;

        /**
         * 验证码图片Base64
         */
        private String imageBase64;
    }
}
