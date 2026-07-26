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

package org.eulerframework.uc.sendgrid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Activates the SendGrid email {@link SendGridEmailOtpChannel} when
 * {@code sendgrid.api-key} is configured. The {@code DelegatingOtpChannel}
 * assembled by {@code OtpChannelConfiguration} routes OTP deliveries with
 * {@code channel=email} to this channel via its explicit routing table.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SendGridProperties.class)
@ConditionalOnProperty(prefix = "sendgrid", name = "api-key")
public class SendGridEmailConfiguration {

    @Bean("sendGridEmailOtpChannel")
    public SendGridEmailOtpChannel sendGridEmailOtpChannel(
            SendGridProperties properties,
            @Qualifier("otpDeliveryTaskExecutor") ThreadPoolTaskExecutor otpDeliveryTaskExecutor) {
        return new SendGridEmailOtpChannel(properties, otpDeliveryTaskExecutor);
    }
}
