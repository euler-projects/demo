package org.eulerframework.demo.controller;

import org.eulerframework.boot.autoconfigure.web.EulerApplicationProperties;
import org.eulerframework.web.config.WebConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("demo")
public class DemoApi {
    @Autowired
    private ConfigurableEnvironment configurableEnvironment;
    @Autowired
    private EulerApplicationProperties eulerApplicationProperties;

    @GetMapping("hello")
    public String hello() {
        return "Euler Boot is running";
    }

    @GetMapping("webConfig")
    public String webConfig() {
        return WebConfig.getRuntimePath();
    }

    @GetMapping("runtimePath")
    public String runtimePath() {
        return eulerApplicationProperties.getRuntimePath();
    }

    @GetMapping("environment/{key}")
    public String runtimePath(@PathVariable String key) {
        return key + ": " + configurableEnvironment.getProperty(key);
    }
}