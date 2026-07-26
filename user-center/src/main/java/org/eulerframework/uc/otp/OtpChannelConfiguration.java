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

package org.eulerframework.uc.otp;

import org.eulerframework.security.authentication.otp.DelegatingOtpChannel;
import org.eulerframework.security.authentication.otp.OtpChannel;
import org.eulerframework.security.authentication.otp.SingleOtpChannel;
import org.eulerframework.security.authentication.otp.StdoutOtpChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Assembles the OTP delivery infrastructure shared by all channels.
 * <p>
 * The gateway channels are conditional beans activated by their respective
 * {@code api-key} properties; this configuration composes whichever are
 * present into a {@link DelegatingOtpChannel} keyed by each channel's
 * self-declared {@link SingleOtpChannel#getChannel() channel name}, marked
 * {@code @Primary} so the OTP configurer's by-type lookup stays unambiguous
 * no matter how many channels are active. When no gateway channel is
 * configured at all, the delegator falls back to {@link StdoutOtpChannel} to
 * keep development setups working.
 */
@Configuration(proxyBeanMethods = false)
public class OtpChannelConfiguration {

    /**
     * Dedicated executor shared by the asynchronous OTP delivery channels.
     * Declared as a non-default candidate so it neither backs off Spring
     * Boot's {@code applicationTaskExecutor} nor captures unqualified
     * {@code Executor} injection points.
     */
    @Bean(defaultCandidate = false)
    public ThreadPoolTaskExecutor otpDeliveryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("otp-delivery-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        // Under saturation deliveries degrade to the caller thread instead of being dropped.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    /**
     * Primary {@link OtpChannel} routing by each gateway's self-declared
     * {@link SingleOtpChannel#getChannel() channel name}, decoupled from
     * bean names. With at least one gateway present there is no fallback, so
     * unknown channel names are rejected synchronously as
     * {@code unsupported_channel}.
     */
    @Bean
    @Primary
    public DelegatingOtpChannel otpChannel(List<SingleOtpChannel> channels) {
        Map<String, OtpChannel> routes = new LinkedHashMap<>();
        channels.forEach(channel -> routes.put(channel.getChannel(), channel));
        if (routes.isEmpty()) {
            // No gateway configured (e.g. local dev without credentials) -
            // print OTPs to the log instead of failing.
            return new DelegatingOtpChannel(routes, new StdoutOtpChannel());
        }
        return new DelegatingOtpChannel(routes);
    }
}
