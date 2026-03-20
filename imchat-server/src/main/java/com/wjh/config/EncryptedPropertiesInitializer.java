package com.wjh.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 在 Spring 容器启动前解密配置文件中的 ENC(...) 属性。
 */
public class EncryptedPropertiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String configKey = resolveConfigKey(environment);
        Map<String, Object> decryptedProperties = new HashMap<String, Object>();

        MutablePropertySources propertySources = environment.getPropertySources();
        for (PropertySource<?> propertySource : propertySources) {
            if (!(propertySource instanceof EnumerablePropertySource)) {
                continue;
            }
            EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
            String[] propertyNames = enumerablePropertySource.getPropertyNames();
            for (String propertyName : propertyNames) {
                if (decryptedProperties.containsKey(propertyName)) {
                    continue;
                }
                Object propertyValue = enumerablePropertySource.getProperty(propertyName);
                if (!(propertyValue instanceof String)) {
                    continue;
                }
                String effectiveValue = environment.getProperty(propertyName);
                if (StringUtils.hasText(effectiveValue) && !propertyValue.equals(effectiveValue)) {
                    continue;
                }
                String decryptedValue = ConfigCryptoUtils.decryptIfNecessary((String) propertyValue, configKey);
                if (!propertyValue.equals(decryptedValue)) {
                    decryptedProperties.put(propertyName, decryptedValue);
                }
            }
        }

        if (!decryptedProperties.isEmpty()) {
            propertySources.addFirst(new MapPropertySource("imchat-decrypted-properties", decryptedProperties));
        }
    }

    private String resolveConfigKey(ConfigurableEnvironment environment) {
        String configKey = environment.getProperty("IMCHAT_CONFIG_KEY");
        if (!StringUtils.hasText(configKey)) {
            configKey = System.getenv("IMCHAT_CONFIG_KEY");
        }
        if (!StringUtils.hasText(configKey)) {
            configKey = System.getProperty("IMCHAT_CONFIG_KEY");
        }
        return configKey;
    }
}
