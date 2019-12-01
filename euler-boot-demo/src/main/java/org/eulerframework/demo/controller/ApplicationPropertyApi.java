package org.eulerframework.demo.controller;

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
