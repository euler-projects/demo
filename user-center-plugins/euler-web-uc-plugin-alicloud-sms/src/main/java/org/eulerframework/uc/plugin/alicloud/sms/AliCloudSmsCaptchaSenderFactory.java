/*
 * Copyright 2013-2024 the original author or authors.
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

package org.eulerframework.uc.plugin.alicloud.sms;

import org.eulerframework.common.util.property.FilePropertySource;
import org.eulerframework.common.util.property.PropertyReader;
import org.eulerframework.web.config.WebConfig;
import org.eulerframework.web.module.authentication.service.SmsCodeValidator.SmsCaptchaSenderFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class AliCloudSmsCaptchaSenderFactory implements SmsCaptchaSenderFactory {
    private final static AliCloudSmsCaptchaSender ALI_CLOUD_SMS_CAPTCHA_SENDER;

    static {
        try {
            PropertyReader ALI_CLOUD_SMS_CONFIG = new PropertyReader(new FilePropertySource("/plugin-alicloud.properties", "file:" + WebConfig.getAdditionalConfigPath() + "/plugin-alicloud.properties"));
            String signName = ALI_CLOUD_SMS_CONFIG.get("alicloud.sms.captcha.signName", String.class, "欧拉框架");
            String templateCode = ALI_CLOUD_SMS_CONFIG.get("alicloud.sms.captcha.templateCode", String.class, "SMS_151773797");
            String accessKeyID = ALI_CLOUD_SMS_CONFIG.get("alicloud.sms.captcha.accessKeyID", String.class, null);
            String accessKeySecret = ALI_CLOUD_SMS_CONFIG.get("alicloud.sms.captcha.accessKeySecret", String.class, null);
            ALI_CLOUD_SMS_CAPTCHA_SENDER = new AliCloudSmsCaptchaSender(signName, templateCode, accessKeyID, accessKeySecret);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SmsCaptchaSender newSmsCaptchaSender() {
        return ALI_CLOUD_SMS_CAPTCHA_SENDER;
    }
}
