package com.wjh.config;

import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 配置项加解密工具。
 */
public final class ConfigCryptoUtils {

    private static final String ENCRYPT_PREFIX = "ENC(";
    private static final String ENCRYPT_SUFFIX = ")";
    private static final int IV_LENGTH = 16;

    private ConfigCryptoUtils() {
    }

    /**
     * 解密形如 ENC(...) 的配置值。
     */
    public static String decryptIfNecessary(String value, String configKey) {
        if (!StringUtils.hasText(value) || !value.startsWith(ENCRYPT_PREFIX) || !value.endsWith(ENCRYPT_SUFFIX)) {
            return value;
        }
        if (!StringUtils.hasText(configKey)) {
            throw new IllegalStateException("检测到加密配置，但未设置 IMCHAT_CONFIG_KEY 环境变量");
        }
        String cipherText = value.substring(ENCRYPT_PREFIX.length(), value.length() - ENCRYPT_SUFFIX.length());
        return decrypt(cipherText, configKey);
    }

    private static String decrypt(String cipherText, String configKey) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(configKey), new IvParameterSpec(iv));
            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("加密配置解密失败，请检查 IMCHAT_CONFIG_KEY 是否正确", e);
        }
    }

    private static SecretKeySpec buildSecretKey(String configKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(configKey.getBytes(StandardCharsets.UTF_8));
        byte[] aesKey = Arrays.copyOf(hash, IV_LENGTH);
        return new SecretKeySpec(aesKey, "AES");
    }
}
