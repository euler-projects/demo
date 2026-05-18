/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eulerframework.uc.yunpian;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Activates the YunPian SMS {@link YunPianSmsOtpChannel} when {@code yunpian.api-key}
 * is configured. The channel is registered under the bean name {@code "sms"} so
 * that {@code DelegatingOtpChannel} can route OTP deliveries with
 * {@code channel=sms} to it.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(YunPianProperties.class)
@ConditionalOnProperty(prefix = "yunpian", name = "api-key")
public class YunPianSmsConfiguration {

    @Bean
    public YunPianSmsOtpChannel yunPianSmsOtpChannel(YunPianProperties properties) {
        return new YunPianSmsOtpChannel(properties);
    }
}
