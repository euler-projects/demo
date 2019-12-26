package org.eulerframework.uc.plugin.alicloud.sms.web.controller;

import org.eulerframework.web.core.annotation.ApiEndpoint;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiEndpoint
@RequestMapping("plugin/ali-cloud/sms/info")
public class PluginInfoApi extends ApiSupportWebController {

    @GetMapping
    public String helloWorld() {
        return "SMS plugin for Alibaba Cloud is enabled";
    }

}
