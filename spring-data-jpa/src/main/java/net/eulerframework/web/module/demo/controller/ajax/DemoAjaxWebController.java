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
package net.eulerframework.web.module.demo.controller.ajax;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;

import net.eulerframework.web.core.annotation.AjaxController;
import net.eulerframework.web.core.base.controller.ApiSupportWebController;

@AjaxController
@RequestMapping("/")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DemoAjaxWebController extends ApiSupportWebController {
    
    @RequestMapping(value = "test") 
    public String test() {
        return "Hello Ajax";
    }
}
