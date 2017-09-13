package net.eulerframework.web.module.demo.controller.ajax;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;

import net.eulerframework.web.core.annotation.AjaxWebController;
import net.eulerframework.web.core.base.controller.AjaxSupportWebController;

@AjaxWebController
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoAjaxWebController extends AjaxSupportWebController {
    
    @RequestMapping(value = "test") 
    public String test() {
        return "Hello Ajax";
    }
}
