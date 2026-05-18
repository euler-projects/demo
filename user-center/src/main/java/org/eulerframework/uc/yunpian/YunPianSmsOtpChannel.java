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

import org.eulerframework.common.http.HttpRequest;
import org.eulerframework.common.http.HttpResponse;
import org.eulerframework.common.http.HttpTemplate;
import org.eulerframework.common.http.JdkHttpClientTemplate;
import org.eulerframework.common.http.ResponseBody;
import org.eulerframework.common.http.request.FormUrlEncodedRequestBody;
import org.eulerframework.security.authentication.otp.OtpChannel;
import org.eulerframework.security.authentication.otp.OtpDelivering;
import org.eulerframework.security.authentication.otp.OtpDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * {@link OtpChannel} backed by the YunPian (云片网) HTTP single-send API.
 * <p>
 * Selects an SMS template by {@link OtpDelivering#purpose() purpose}, falling
 * back to the {@link YunPianProperties#DEFAULT_TEMPLATE_KEY default} template
 * when the purpose is empty or has no dedicated entry. The chosen template is
 * rendered by substituting {@code {code}} with the OTP value and
 * {@code {minutes}} with the OTP validity expressed in (rounded-up) minutes.
 *
 * @see <a href="https://www.yunpian.com/official/document/sms/zh_cn/domestic_single_send">
 * YunPian single-send API</a>
 */
public class YunPianSmsOtpChannel implements OtpChannel {

    private static final Logger logger = LoggerFactory.getLogger(YunPianSmsOtpChannel.class);

    private static final String CHANNEL_NAME = "sms";
    private static final String PLACEHOLDER_CODE = "{code}";
    private static final String PLACEHOLDER_MINUTES = "{minutes}";

    private final HttpTemplate httpTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final Map<String, String> templates;

    public YunPianSmsOtpChannel(YunPianProperties properties) {
        this(properties, JdkHttpClientTemplate.INSTANCE);
    }

    public YunPianSmsOtpChannel(YunPianProperties properties, HttpTemplate httpTemplate) {
        Assert.notNull(properties, "properties must not be null");
        Assert.hasText(properties.getApiKey(), "yunpian.api-key must be configured");
        Assert.hasText(properties.getApiUrl(), "yunpian.api-url must be configured");
        Assert.notNull(properties.getTemplates(), "yunpian.templates must not be null");
        Assert.hasText(properties.getTemplates().get(YunPianProperties.DEFAULT_TEMPLATE_KEY),
                "yunpian.templates." + YunPianProperties.DEFAULT_TEMPLATE_KEY
                        + " is required as the fallback template");
        Assert.notNull(httpTemplate, "httpTemplate must not be null");
        this.apiKey = properties.getApiKey();
        this.apiUrl = properties.getApiUrl();
        this.templates = Map.copyOf(properties.getTemplates());
        this.httpTemplate = httpTemplate;
    }

    @Override
    public void send(OtpDelivering delivering) throws OtpDeliveryException {
        Assert.isTrue(CHANNEL_NAME.equalsIgnoreCase(delivering.channel()),
                "YunPianSmsOtpChannel only support sms channel.");

        String text = renderText(delivering);
        doSend(this.httpTemplate, this.apiUrl, this.apiKey, delivering.recipient(), text);
    }

    public static void doSend(HttpTemplate httpTemplate, String apiUrl, String apiKey, String phone, String message) throws OtpDeliveryException {
        FormUrlEncodedRequestBody body = FormUrlEncodedRequestBody.newBuilder()
                .add("apikey", apiKey)
                .add("mobile", phone)
                .add("text", message)
                .build();

        try (HttpResponse response = httpTemplate.execute(
                HttpRequest.post(apiUrl)
                        .header("Accept", "application/json;charset=utf-8")
                        .body(body)
                        .build())) {
            String payload = readBody(response);

            if (response.getStatus() != 200) {
                throw new OtpDeliveryException("YunPian sms request failed with HTTP "
                        + response.getStatus() + ", body=" + payload);
            }

            // YunPian returns {"code":0,...} on success; any non-zero code indicates failure.
            if (!isSuccess(payload)) {
                throw new OtpDeliveryException("YunPian sms delivery failed: " + payload);
            }

            System.out.println(payload);
        } catch (IOException | URISyntaxException e) {
            throw new OtpDeliveryException("Failed to call YunPian single_send API", e);
        }
    }

    private String renderText(OtpDelivering delivering) {
        String template = pickTemplate(delivering.purpose());
        return template
                .replace(PLACEHOLDER_CODE, delivering.otp());
    }

    private String pickTemplate(String purpose) {
        if (StringUtils.hasText(purpose)) {
            String template = this.templates.get(purpose);
            if (StringUtils.hasText(template)) {
                return template;
            }
        }
        return this.templates.get(YunPianProperties.DEFAULT_TEMPLATE_KEY);
    }

    private static String readBody(HttpResponse response) throws IOException {
        ResponseBody responseBody = response.getBody();
        if (responseBody == null) {
            return "";
        }
        String text = responseBody.asText();
        return text == null ? "" : text;
    }

    private static boolean isSuccess(String payload) {
        if (payload == null) {
            return false;
        }
        // Avoid pulling Jackson here; the canonical success marker is "code":0.
        String stripped = payload.replaceAll("\\s+", "");
        return stripped.contains("\"code\":0");
    }
}
