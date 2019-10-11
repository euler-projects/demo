package org.eulerframework.uc.web.controller.ajax;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;

import org.eulerframework.web.core.annotation.AjaxController;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;

@AjaxController
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoAjaxWebController extends ApiSupportWebController {
    
    @RequestMapping(value = "test") 
    public String test() {
        return "Hello Ajax";
    }
}
