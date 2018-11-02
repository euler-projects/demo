/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.web.module.demo.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;

import org.eulerframework.web.core.annotation.ApiEndpoint;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.eulerframework.web.module.demo.MobileCodeRobotCheckService;

@ApiEndpoint
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoApiEndpoint extends ApiSupportWebController {
    
    @Autowired
    private MobileCodeRobotCheckService mobileCodeRobotCheckService;
    
    @RequestMapping(value = "sendSmsCode") 
    public void sendSmsCode() {
        this.mobileCodeRobotCheckService.sendSmsCode(this.getRequest());
    }
}
