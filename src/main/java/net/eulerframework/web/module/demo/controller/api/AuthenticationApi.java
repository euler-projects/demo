/**
 * 
 */
package net.eulerframework.web.module.demo.controller.api;

import org.springframework.web.bind.annotation.RequestMapping;

import net.eulerframework.web.config.WebConfig;
import net.eulerframework.web.core.annotation.ApiEndpoint;
import net.eulerframework.web.core.base.controller.AbstractApiEndpoint;

/**
 * @author cFrost
 *
 */
@ApiEndpoint
@RequestMapping("/")
public class AuthenticationApi extends AbstractApiEndpoint {
    
    @RequestMapping(value = "clean") 
    public void clean() {
        WebConfig.clearWebConfigCache();
    }
    
    @RequestMapping(value = "test") 
    public void test() {
         System.out.println(WebConfig.getAdminJspPath());
         System.out.println(WebConfig.getAdminRootPath());
         System.out.println(WebConfig.getApiAuthenticationType());
         System.out.println(WebConfig.getApiRootPath());
         System.out.println(WebConfig.getEmailFormat());
         System.out.println(WebConfig.getI18nRefreshFreq());
         System.out.println(WebConfig.getJspPath());
         System.out.println(WebConfig.getMinPasswordLength());
         System.out.println(WebConfig.getMultiPartConfig());
         System.out.println(WebConfig.getOAuthSeverType());
         System.out.println(WebConfig.getPasswordFormat());
         System.out.println(WebConfig.getRamCacheCleanFreq());
         System.out.println(WebConfig.getUploadPath());
         System.out.println(WebConfig.getUserContextCacheLife());
         System.out.println(WebConfig.getUsernameFormat());
         System.out.println(WebConfig.getWebAuthenticationType());
        
    }

}
