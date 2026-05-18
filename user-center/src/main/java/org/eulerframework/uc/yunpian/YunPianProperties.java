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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration properties for the YunPian SMS gateway, bound under the
 * {@code yunpian.*} namespace.
 * <p>
 * Templates are keyed by an opaque purpose tag matching
 * {@link org.eulerframework.security.authentication.otp.OtpDelivering#purpose()};
 * the entry under {@link #DEFAULT_TEMPLATE_KEY} is required and used as the
 * fallback when the incoming purpose is missing or has no dedicated template.
 * <p>
 * Each template body may contain the placeholders {@code {code}} and
 * {@code {minutes}}, which are substituted with the OTP value and the OTP
 * validity in minutes respectively at delivery time.
 */
@ConfigurationProperties(prefix = "yunpian")
public class YunPianProperties {

    /**
     * Key used to look up the fallback template inside {@link #templates}.
     */
    public static final String DEFAULT_TEMPLATE_KEY = "default";

    /**
     * Default endpoint of the YunPian single-send API.
     */
    public static final String DEFAULT_API_URL = "https://sms.yunpian.com/v2/sms/single_send.json";

    /**
     * YunPian {@code apikey}; can be obtained from the YunPian console.
     */
    private String apiKey;

    /**
     * Override of the YunPian single-send endpoint URL. Defaults to
     * {@link #DEFAULT_API_URL}; overseas deployments may switch to
     * {@code https://us.yunpian.com/v2/sms/single_send.json}.
     */
    private String apiUrl = DEFAULT_API_URL;

    /**
     * Templates keyed by {@code purpose}. The entry named
     * {@link #DEFAULT_TEMPLATE_KEY} is required and acts as the fallback when
     * an incoming {@code purpose} is empty or unknown.
     */
    private Map<String, String> templates = new LinkedHashMap<>();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }
}
