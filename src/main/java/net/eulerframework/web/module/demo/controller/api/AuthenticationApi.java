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
        
    }

}
