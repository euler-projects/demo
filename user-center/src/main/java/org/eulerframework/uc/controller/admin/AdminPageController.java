package org.eulerframework.uc.controller.admin;

import org.eulerframework.security.web.endpoint.EulerSecurityEndpoints;
import org.eulerframework.web.core.base.controller.PageRender;
import org.eulerframework.web.core.base.controller.PageSupportWebController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AdminPageController extends PageSupportWebController {
    private String logoutProcessingUrl;

    protected AdminPageController(PageRender pageRender) {
        super(pageRender);
    }

    @PreAuthorize("hasAnyAuthority('root', 'admin')")
    @GetMapping(value = {"admin/console", "admin/console/**"}, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView adminPage() {
        return this.display("admin/index", false);
    }

    @ModelAttribute("logoutProcessingUrl")
    public String getLogoutProcessingUrl() {
        return this.logoutProcessingUrl;
    }

    @Value("${" + EulerSecurityEndpoints.USER_LOGOUT_PROCESSING_URL_PROP_NAME + ":" + EulerSecurityEndpoints.USER_LOGOUT_PROCESSING_URL + "}")
    public void setLogoutProcessingUrl(String logoutProcessingUrl) {
        this.logoutProcessingUrl = logoutProcessingUrl;
    }
}
