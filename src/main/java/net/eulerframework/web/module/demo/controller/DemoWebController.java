package net.eulerframework.web.module.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import net.eulerframework.web.core.annotation.WebController;
import net.eulerframework.web.core.base.controller.JspSupportWebController;

@WebController
@RequestMapping("/")
public class DemoWebController extends JspSupportWebController {
    
    @RequestMapping(value = { "", "/", "index" }, method = RequestMethod.GET)
    public String index() {
        return this.display("index");
    }
}
