package net.eulerframework.web.module.demo.controller;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import net.eulerframework.web.core.annotation.WebController;
import net.eulerframework.web.core.base.controller.JspSupportWebController;

@WebController
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoAjaxWebController extends JspSupportWebController {
    
}
