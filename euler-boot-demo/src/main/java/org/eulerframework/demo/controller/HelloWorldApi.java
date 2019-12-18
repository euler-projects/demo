/*
 * Copyright 2013-2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.eulerframework.demo.controller;

import org.eulerframework.constant.EulerSysAttributes;
import org.eulerframework.web.config.WebConfig;
import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.eulerframework.web.util.ServletUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("helloWorld")
public class HelloWorldApi extends ApiSupportWebController {

    @RequestMapping
    public Object helloWorld() {
        Map<String, Object> m = new HashMap<>();

        Set<String> eulerSysAttributeNames = EulerSysAttributes.getEulerSysAttributeNames();

        Enumeration<String> attributeNames = ServletUtils.getServletContext().getAttributeNames();
        while(attributeNames.hasMoreElements()) {
            String arrtibuteName = attributeNames.nextElement();
            if(eulerSysAttributeNames.contains(arrtibuteName)) {
                m.put(arrtibuteName, this.getServletContext().getAttribute(arrtibuteName));
            }
        }

        HttpSession session = this.getRequest().getSession();

        if(session != null) {
            attributeNames = this.getRequest().getSession().getAttributeNames();
            while(attributeNames.hasMoreElements()) {
                String arrtibuteName = attributeNames.nextElement();
                if(eulerSysAttributeNames.contains(arrtibuteName)) {
                    m.put(arrtibuteName, this.getRequest().getSession().getAttribute(arrtibuteName));
                }
            }
        }

        attributeNames = this.getRequest().getAttributeNames();
        while(attributeNames.hasMoreElements()) {
            String arrtibuteName = attributeNames.nextElement();
            if(eulerSysAttributeNames.contains(arrtibuteName)) {
                m.put(arrtibuteName, this.getRequest().getAttribute(arrtibuteName));
            }
        }

        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        Enumeration<String> headerNames = this.getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headers = this.getRequest().getHeaders(headerName);
            List<String> headerList = new ArrayList<>();
            while (headers.hasMoreElements()) {
                headerList.add(headers.nextElement());
            }
            headerMap.put(headerName, headerList);
        }


        Map<String, Object> result = new LinkedHashMap<>();
        result.put("systemAttributes", m);
        result.put("headers", headerMap);
        return result;
    }
}
