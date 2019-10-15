package org.eulerframework.uc.web.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.eulerframework.web.module.oauth2.service.EulerOAuth2ClientEntityService;
import org.eulerframework.web.module.oauth2.service.MockOAuth2ClientEntityService;

@Configuration
public class UserCenterBeans {
    
    @Bean
    public EulerOAuth2ClientEntityService eulerOAuth2ClientEntityService(PasswordEncoder passwordEncoder) {
        return new MockOAuth2ClientEntityService(passwordEncoder);
    }
}
