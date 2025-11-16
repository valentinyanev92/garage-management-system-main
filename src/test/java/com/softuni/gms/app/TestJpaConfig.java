package com.softuni.gms.app;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        RedisAutoConfiguration.class,
        WebMvcAutoConfiguration.class
})
public class TestJpaConfig {
}
