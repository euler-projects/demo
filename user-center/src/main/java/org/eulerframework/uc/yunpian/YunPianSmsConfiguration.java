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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Activates the YunPian SMS {@link YunPianSmsOneTimePasswordChannel} when {@code yunpian.api-key}
 * is configured. The {@code DelegatingOneTimePasswordChannel} assembled by
 * {@code OneTimePasswordChannelConfiguration} routes OTP deliveries with
 * {@code channel=sms} to this channel via its explicit routing table.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(YunPianProperties.class)
@ConditionalOnProperty(prefix = "yunpian", name = "api-key")
public class YunPianSmsConfiguration {

    @Bean("yunPianSmsOneTimePasswordChannel")
    public YunPianSmsOneTimePasswordChannel yunPianSmsOneTimePasswordChannel(
            YunPianProperties properties,
            @Qualifier("otpDeliveryTaskExecutor") ThreadPoolTaskExecutor otpDeliveryTaskExecutor) {
        return new YunPianSmsOneTimePasswordChannel(properties, otpDeliveryTaskExecutor);
    }
}
