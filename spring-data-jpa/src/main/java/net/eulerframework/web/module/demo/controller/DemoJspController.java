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
