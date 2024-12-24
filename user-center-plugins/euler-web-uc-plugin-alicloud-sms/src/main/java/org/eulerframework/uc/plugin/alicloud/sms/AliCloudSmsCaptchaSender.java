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

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.eulerframework.web.module.authentication.service.SmsCodeValidator.BizCode;
import org.eulerframework.web.module.authentication.service.SmsCodeValidator.SmsCaptchaSenderFactory.SmsCaptchaSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliCloudSmsCaptchaSender implements SmsCaptchaSender {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String signName;
    private final String templateCode;
    private final IAcsClient client;

    public AliCloudSmsCaptchaSender(String signName, String templateCode, String accessKeyId, String accessSecret) {
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessSecret);
        this.signName = signName;
        this.templateCode = templateCode;
        client = new DefaultAcsClient(profile);

        this.logger.info("Alibaba Cloud SMS captcha sender initialized. Sign Name: {}, Template Code: {}", this.signName, this.templateCode);
    }

    @Override
    public void sendSms(BizCode bizCode, String mobile, String captcha, int expireMinutes) {
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", "cn-hangzhou");
        request.putQueryParameter("PhoneNumbers", mobile);
        request.putQueryParameter("SignName", this.signName);
        request.putQueryParameter("TemplateCode", this.templateCode);
        request.putQueryParameter("TemplateParam", "{\"code\":\"" + captcha+ "\"}");
        try {
            CommonResponse response = client.getCommonResponse(request);
            this.logger.info("Alibaba Cloud SMS send response: {}", response.getData());
        } catch (ClientException e) {
            e.printStackTrace();
        }

    }
}
