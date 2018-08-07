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
package net.eulerframework.web.module.demo.controller;

import java.util.Locale;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import net.eulerframework.web.config.WebConfig;
import net.eulerframework.web.core.annotation.JspController;
import net.eulerframework.web.core.base.controller.JspSupportWebController;

@JspController
@RequestMapping("/")
public class DemoJspController extends JspSupportWebController {

    @RequestMapping(value = { "", "/", "index" }, method = RequestMethod.GET)
    public String index(Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String localeStr = "/";

        if (StringUtils.hasText(language) && StringUtils.hasText(country)) {
            localeStr += language.toLowerCase() + "-" + country.toLowerCase();
        } else if (StringUtils.hasText(language)) {
            localeStr += language.toLowerCase();
        } else if (StringUtils.hasText(country)) {
            localeStr += country.toLowerCase();
        }

        return this.redirect(WebConfig.getStaticPagesRootPath() + localeStr + "/index.html");
    }

    @RequestMapping(path = "site-map", method = RequestMethod.GET)
    public String siteMap() {
        return this.display("index");
    }
}
