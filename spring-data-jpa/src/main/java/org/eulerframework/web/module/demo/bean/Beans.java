/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.web.module.demo.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Beans {

    private final static String EMAIL_SIGN = 
            "<p>------------------</p>"
            + "<p>Best Regards</p>";

    private final static String RESET_PASSWORD_EMAIL_CONTENT = 
            "<p>尊敬的先生/女士，</p>"
            + "<p>您可使用下面的链接重置密码，十分钟内有效：</p>"
            + "<p><a href=\"${resetPasswordUrl}\">${resetPasswordUrl}</a></p>"
            + "<br>"
            + "<p>Dear Sir/Madam,</p>"
            + "<p>You can use the following link to reset your password in 10 minutes:</p>"
            + "<p><a href=\"${resetPasswordUrl}\">${resetPasswordUrl}</a></p>"
            + EMAIL_SIGN;

    @Bean
    public String resetPasswordEmailContent() {
        return RESET_PASSWORD_EMAIL_CONTENT;
    }

    @Bean
    public String resetPasswordEmailSubject() {
        return "[Euler Projects] Please reset your password";
    }
}
