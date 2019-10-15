package org.eulerframework.uc.web.controller.api;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.eulerframework.web.core.annotation.ApiEndpoint;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.eulerframework.web.module.authentication.context.UserContext;
import org.eulerframework.web.module.authentication.principal.EulerUserDetails;

@ApiEndpoint
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoApiEndpoint extends ApiSupportWebController {
    
    @GetMapping("whoami")
    public EulerUserDetails whoami() {
        return UserContext.getCurrentUser();
    }
}
