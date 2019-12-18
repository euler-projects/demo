package org.eulerframework.demo.controller;

import org.eulerframework.web.config.MultipartConfig;
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

    @GetMapping("additionalConfigPath")
    public String additionalConfigPath() {
        return WebConfig.getAdditionalConfigPath();
    }

    @GetMapping("supportLanguages")
    public Locale[] supportLanguages() {
        return WebConfig.getSupportLanguages();
    }

    @GetMapping("multipart/location")
    public String multipartLocation() {
        return WebConfig.getMultipartConfig().getLocation();
    }

    @GetMapping("multipart/fileSizeThreshold")
    public long multipartFileSizeThreshold() {
        return WebConfig.getMultipartConfig().getFileSizeThreshold().toBytes();
    }

    @GetMapping("multipart/maxRequestSize")
    public long multipartMaxRequestSize() {
        return WebConfig.getMultipartConfig().getMaxRequestSize().toBytes();
    }

    @GetMapping("multipart/maxFileSize")
    public long multipartMaxFileSize() {
        return WebConfig.getMultipartConfig().getMaxFileSize().toBytes();
    }
}
