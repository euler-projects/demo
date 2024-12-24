/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
