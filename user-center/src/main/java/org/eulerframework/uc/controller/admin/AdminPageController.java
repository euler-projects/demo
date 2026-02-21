package org.eulerframework.uc.controller.admin;

import org.eulerframework.web.core.base.controller.PageRender;
import org.eulerframework.web.core.base.controller.PageSupportWebController;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AdminPageController extends PageSupportWebController {
    protected AdminPageController(PageRender pageRender) {
        super(pageRender);
    }

    @PreAuthorize("hasAnyAuthority('root', 'admin')")
    @GetMapping(value = {"admin", "admin/*"}, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView adminPage() {
        return this.display("admin/index", false);
    }
}
