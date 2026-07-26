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

import org.eulerframework.common.http.ContentType;
import org.eulerframework.common.http.HttpRequest;
import org.eulerframework.common.http.HttpResponse;
import org.eulerframework.common.http.HttpTemplate;
import org.eulerframework.common.http.JdkHttpClientTemplate;
import org.eulerframework.common.http.ResponseBody;
import org.eulerframework.common.http.request.StringRequestBody;
import org.eulerframework.common.util.jackson.JacksonUtils;
import org.eulerframework.security.authentication.otp.AbstractAsyncOtpChannel;
import org.eulerframework.security.authentication.otp.OtpChannel;
import org.eulerframework.security.authentication.otp.OtpDelivering;
import org.eulerframework.security.authentication.otp.OtpDeliveryException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * {@link OtpChannel} backed by the SendGrid v3 mail send API. Extends
 * {@link AbstractAsyncOtpChannel}, so the blocking HTTP call runs on the
 * injected executor and never blocks the OTP issue request thread.
 * <p>
 * Selects a SendGrid dynamic template by
 * {@link OtpDelivering#purpose() purpose}, falling back to the
 * {@link SendGridProperties#DEFAULT_TEMPLATE_KEY default} template when the
 * purpose is empty or has no dedicated entry. The template receives
 * {@code code} (the OTP value) and {@code minutes} (the OTP validity in
 * rounded-up minutes) as dynamic template data.
 *
 * @see <a href="https://www.twilio.com/docs/sendgrid/api-reference/mail-send/mail-send">
 * SendGrid v3 mail send API</a>
 */
public class SendGridEmailOtpChannel extends AbstractAsyncOtpChannel {

    private static final String CHANNEL_NAME = "email";

    private final HttpTemplate httpTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final SendGridProperties.From from;
    private final Map<String, String> templates;

    public SendGridEmailOtpChannel(SendGridProperties properties, Executor executor) {
        this(properties, JdkHttpClientTemplate.INSTANCE, executor);
    }

    public SendGridEmailOtpChannel(SendGridProperties properties, HttpTemplate httpTemplate, Executor executor) {
        super(executor);
        Assert.notNull(properties, "properties must not be null");
        Assert.hasText(properties.getApiKey(), "sendgrid.api-key must be configured");
        Assert.hasText(properties.getApiUrl(), "sendgrid.api-url must be configured");
        Assert.notNull(properties.getFrom(), "sendgrid.from must not be null");
        Assert.hasText(properties.getFrom().getEmail(), "sendgrid.from.email must be configured");
        Assert.notNull(properties.getTemplates(), "sendgrid.templates must not be null");
        Assert.hasText(properties.getTemplates().get(SendGridProperties.DEFAULT_TEMPLATE_KEY),
                "sendgrid.templates." + SendGridProperties.DEFAULT_TEMPLATE_KEY
                        + " is required as the fallback template");
        Assert.notNull(httpTemplate, "httpTemplate must not be null");
        this.apiKey = properties.getApiKey();
        this.apiUrl = properties.getApiUrl();
        this.from = properties.getFrom();
        this.templates = Map.copyOf(properties.getTemplates());
        this.httpTemplate = httpTemplate;
    }

    @Override
    public String getChannel() {
        return CHANNEL_NAME;
    }

    @Override
    protected void doSend(OtpDelivering delivering) throws OtpDeliveryException {
        // Defensive re-check; unsupported channels are already rejected
        // synchronously by AbstractAsyncOtpChannel via supports().
        Assert.isTrue(CHANNEL_NAME.equalsIgnoreCase(delivering.channel()),
                "SendGridEmailOtpChannel only support email channel.");

        StringRequestBody body = new StringRequestBody(
                JacksonUtils.writeValueAsString(buildPayload(delivering)),
                ContentType.APPLICATION_JSON);

        try (HttpResponse response = this.httpTemplate.execute(
                HttpRequest.post(this.apiUrl)
                        .header("Authorization", "Bearer " + this.apiKey)
                        .body(body)
                        .build())) {
            // SendGrid answers 202 Accepted with an empty body on success.
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new OtpDeliveryException("SendGrid mail request failed with HTTP "
                        + response.getStatus() + ", body=" + readBody(response));
            }
        } catch (IOException | URISyntaxException e) {
            throw new OtpDeliveryException("Failed to call SendGrid mail send API", e);
        }
    }

    /**
     * Assemble the SendGrid v3 mail send payload; serialised by Jackson so
     * recipient addresses and template data are always properly escaped.
     */
    private Map<String, Object> buildPayload(OtpDelivering delivering) {
        Map<String, Object> dynamicTemplateData = new LinkedHashMap<>();
        dynamicTemplateData.put("code", delivering.otp());
        // Rounded-up minutes, e.g. a 90s TTL renders as "2".
        dynamicTemplateData.put("minutes",
                String.valueOf((delivering.expiresIn().toSeconds() + 59) / 60));

        Map<String, Object> fromNode = new LinkedHashMap<>();
        fromNode.put("email", this.from.getEmail());
        if (StringUtils.hasText(this.from.getName())) {
            fromNode.put("name", this.from.getName());
        }

        return Map.of(
                "personalizations", List.of(Map.of(
                        "dynamic_template_data", dynamicTemplateData,
                        "to", List.of(Map.of("email", delivering.recipient())))),
                "from", fromNode,
                "template_id", pickTemplate(delivering.purpose()));
    }

    private String pickTemplate(String purpose) {
        if (StringUtils.hasText(purpose)) {
            String template = this.templates.get(purpose);
            if (StringUtils.hasText(template)) {
                return template;
            }
        }
        return this.templates.get(SendGridProperties.DEFAULT_TEMPLATE_KEY);
    }

    private static String readBody(HttpResponse response) throws IOException {
        ResponseBody responseBody = response.getBody();
        if (responseBody == null) {
            return "";
        }
        String text = responseBody.asText();
        return text == null ? "" : text;
    }
}
