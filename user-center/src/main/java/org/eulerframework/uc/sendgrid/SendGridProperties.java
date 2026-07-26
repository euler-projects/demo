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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration properties for the SendGrid mail gateway, bound under the
 * {@code sendgrid.*} namespace.
 * <p>
 * Templates are keyed by an opaque purpose tag matching
 * {@link org.eulerframework.security.authentication.otp.OtpDelivering#purpose()};
 * the entry under {@link #DEFAULT_TEMPLATE_KEY} is required and used as the
 * fallback when the incoming purpose is missing or has no dedicated template.
 * <p>
 * Each entry is a SendGrid <em>dynamic template id</em> ({@code d-...}); the
 * template receives {@code code} (the OTP value) and {@code minutes} (the OTP
 * validity in rounded-up minutes) as dynamic template data.
 */
@ConfigurationProperties(prefix = "sendgrid")
public class SendGridProperties {

    /**
     * Key used to look up the fallback template inside {@link #templates}.
     */
    public static final String DEFAULT_TEMPLATE_KEY = "default";

    /**
     * Default endpoint of the SendGrid v3 mail send API.
     */
    public static final String DEFAULT_API_URL = "https://api.sendgrid.com/v3/mail/send";

    /**
     * SendGrid API key; can be obtained from the SendGrid console and is sent
     * as an {@code Authorization: Bearer} header.
     */
    private String apiKey;

    /**
     * Override of the SendGrid mail send endpoint URL. Defaults to
     * {@link #DEFAULT_API_URL}.
     */
    private String apiUrl = DEFAULT_API_URL;

    /**
     * Sender identity; the address must be verified in SendGrid.
     */
    private From from = new From();

    /**
     * Dynamic template ids keyed by {@code purpose}. The entry named
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

    public From getFrom() {
        return from;
    }

    public void setFrom(From from) {
        this.from = from;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }

    /**
     * Sender identity carrier ({@code from.email} / {@code from.name}).
     */
    public static class From {

        /**
         * Sender email address; required, must be a verified sender in
         * SendGrid.
         */
        private String email;

        /**
         * Optional human-readable sender name displayed by mail clients.
         */
        private String name;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
