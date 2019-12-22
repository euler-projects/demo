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
