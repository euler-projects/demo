package org.eulerframework.demo.controller;

import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("property/environment")
public class EnvironmentPropertyApi extends ApiSupportWebController {
    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @GetMapping
    public String property(@RequestParam String key) {
        return configurableEnvironment.getProperty(key);
    }
}
