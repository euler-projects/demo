package org.eulerframework.demo.controller;

import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebSiteProperties;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("property/web")
public class WebPropertyApi extends ApiSupportWebController {
    @Autowired
    private EulerWebSiteProperties eulerWebSiteProperties;

    @GetMapping
    public EulerWebSiteProperties webProperties() {
        return eulerWebSiteProperties;
    }
}
