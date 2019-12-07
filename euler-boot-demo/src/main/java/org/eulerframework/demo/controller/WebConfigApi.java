package org.eulerframework.demo.controller;

import org.eulerframework.web.config.WebConfig;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("webConfig")
public class WebConfigApi extends ApiSupportWebController {

    @GetMapping("applicationName")
    public String applicationName() {
        return WebConfig.getApplicationName();
    }

    @GetMapping("runtimePath")
    public String runtimePath() {
        return WebConfig.getRuntimePath();
    }

    @GetMapping("tempPath")
    public String tempPath() {
        return WebConfig.getTempPath();
    }

    @GetMapping("supportLanguages")
    public Locale[] supportLanguages() {
        return WebConfig.getSupportLanguages();
    }
}
