package org.eulerframework.uc.config;

import org.eulerframework.security.authentication.apple.AppleAppAttestKeyCredentialService;
import org.eulerframework.security.authentication.apple.RedisAppleAppAttestKeyCredentialService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AppleAppAttestConfig {

    @Bean
    public AppleAppAttestKeyCredentialService appleAppAttestKeyCredentialService(StringRedisTemplate redisTemplate) {
        return new RedisAppleAppAttestKeyCredentialService(redisTemplate);
    }
}
