package net.eulerframework.web.module.demo.bean;

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
