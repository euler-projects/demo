package net.eulerframework.web.module.demo.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;

import net.eulerframework.web.core.annotation.ApiEndpoint;
import net.eulerframework.web.core.base.controller.ApiSupportWebController;
import net.eulerframework.web.module.demo.MobileCodeRobotCheckService;

@ApiEndpoint
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoApiEndpoint extends ApiSupportWebController {
    
    @Autowired
    private MobileCodeRobotCheckService mobileCodeRobotCheckService;
    
    @RequestMapping(value = "sendSmsCode") 
    public void sendSmsCode() {
        this.mobileCodeRobotCheckService.sendSmsCode(this.getRequest());
    }
}
