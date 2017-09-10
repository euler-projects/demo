package net.eulerframework.web.module.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import net.eulerframework.web.core.annotation.WebController;
import net.eulerframework.web.core.base.controller.JspSupportWebController;
import net.eulerframework.web.module.authentication.context.UserContext;

@WebController
@RequestMapping("/")
public class DemoWebController extends JspSupportWebController {
    
    @RequestMapping(value = { "", "/", "index" }, method = RequestMethod.GET)
    public String index() {
        System.out.println(UserContext.getCurrentUser().getUsername());
        return this.redirect("/dashboard");
    }
}
