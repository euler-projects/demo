package net.eulerframework.web.module.demo.controller.ajax;

import javax.inject.Inject;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;

import net.eulerframework.web.core.annotation.AjaxController;
import net.eulerframework.web.core.base.controller.AjaxSupportWebController;
import net.eulerframework.web.module.demo.entity.User;
import net.eulerframework.web.module.demo.service.TestService;

@AjaxController
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoAjaxWebController extends AjaxSupportWebController {
    
    @Inject TestService testService;
    
    @RequestMapping(value = "test") 
    public User test(String userId) {
        return this.testService.findUser(userId);
    }
}
