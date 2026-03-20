package com.wjh.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 验证码存储服务，Redis 不可用时自动回退到本地内存。
 */
@Slf4j
@Service
public class CaptchaStoreService {

    private static final String CAPTCHA_PREFIX = "captcha:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Map<String, LocalCaptchaValue> localCaptchaCache = new ConcurrentHashMap<String, LocalCaptchaValue>();
    private final AtomicBoolean redisWarningLogged = new AtomicBoolean(false);

    /**
     * 保存验证码
     */
    public void save(String captchaKey, String captchaCode, long expireMinutes) {
        String fullKey = buildKey(captchaKey);
        try {
            stringRedisTemplate.opsForValue().set(fullKey, captchaCode, expireMinutes, TimeUnit.MINUTES);
            return;
        } catch (Exception e) {
            logRedisFallbackOnce(e);
        }
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(expireMinutes);
        localCaptchaCache.put(fullKey, new LocalCaptchaValue(captchaCode, expireAt));
    }

    /**
     * 读取验证码
     */
    public String get(String captchaKey) {
        String fullKey = buildKey(captchaKey);
        try {
            String captchaCode = stringRedisTemplate.opsForValue().get(fullKey);
            if (captchaCode != null) {
                return captchaCode;
            }
        } catch (Exception e) {
            logRedisFallbackOnce(e);
        }
        LocalCaptchaValue localCaptchaValue = localCaptchaCache.get(fullKey);
        if (localCaptchaValue == null) {
            return null;
        }
        if (localCaptchaValue.getExpireAt() < System.currentTimeMillis()) {
            localCaptchaCache.remove(fullKey);
            return null;
        }
        return localCaptchaValue.getCaptchaCode();
    }

    /**
     * 删除验证码
     */
    public void delete(String captchaKey) {
        String fullKey = buildKey(captchaKey);
        try {
            stringRedisTemplate.delete(fullKey);
        } catch (Exception e) {
            logRedisFallbackOnce(e);
        }
        localCaptchaCache.remove(fullKey);
    }

    private String buildKey(String captchaKey) {
        return CAPTCHA_PREFIX + captchaKey;
    }

    private void logRedisFallbackOnce(Exception e) {
        if (redisWarningLogged.compareAndSet(false, true)) {
            log.warn("Redis 不可用，验证码存储已回退到本地内存: {}", e.getMessage());
        }
    }

    /**
     * 本地验证码缓存对象
     */
    private static class LocalCaptchaValue {
        private final String captchaCode;
        private final long expireAt;

        private LocalCaptchaValue(String captchaCode, long expireAt) {
            this.captchaCode = captchaCode;
            this.expireAt = expireAt;
        }

        private String getCaptchaCode() {
            return captchaCode;
        }

        private long getExpireAt() {
            return expireAt;
        }
    }
}
