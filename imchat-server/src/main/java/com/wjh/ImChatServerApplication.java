package com.wjh;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.wjh.config.EncryptedPropertiesInitializer;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.wjh.mapper")
public class ImChatServerApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ImChatServerApplication.class);
        application.addInitializers(new EncryptedPropertiesInitializer());
        application.run(args);
    }

}
