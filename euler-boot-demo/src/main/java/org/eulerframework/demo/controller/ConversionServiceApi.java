package org.eulerframework.demo.controller;

import org.eulerframework.web.core.base.controller.ApiSupportWebController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("convert")
public class ConversionServiceApi extends ApiSupportWebController {
    @Autowired
    private ConversionService conversionService;

    @GetMapping("duration")
    public Duration stringToDuration(@RequestParam String value) {
        return this.conversionService.convert(value, Duration.class);
    }

}
