package org.eulerframework.demo.controller;

import org.eulerframework.boot.autoconfigure.web.EulerApplicationProperties;
import org.eulerframework.web.config.WebConfig;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("property/application")
public class ApplicationPropertyApi extends ApiSupportWebController {
    @Autowired
    private EulerApplicationProperties eulerApplicationProperties;

    @GetMapping
    public EulerApplicationProperties applicationProperties() {
        return eulerApplicationProperties;
    }
}
