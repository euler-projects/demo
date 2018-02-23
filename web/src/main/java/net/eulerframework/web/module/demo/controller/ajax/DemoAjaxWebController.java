package net.eulerframework.web.module.demo.controller.ajax;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;

import net.eulerframework.web.core.annotation.AjaxController;
import net.eulerframework.web.core.base.controller.AjaxSupportWebController;

@AjaxController
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoAjaxWebController extends AjaxSupportWebController {
    
    @RequestMapping(value = "test") 
    public String test() {
        return "Hello Ajax";
    }
}
