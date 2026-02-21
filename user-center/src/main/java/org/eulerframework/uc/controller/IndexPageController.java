package org.eulerframework.uc.controller;

import jakarta.annotation.Resource;
import org.eulerframework.context.ApplicationContextHolder;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.web.core.base.controller.PageSupportWebController;
import org.eulerframework.web.core.base.controller.PageRender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.ArrayList;
@Controller
@RequestMapping
public class IndexPageController extends PageSupportWebController {
    @Resource
    private UserContext userContext;

    @Autowired
    public IndexPageController(PageRender pageRender) {
        super(pageRender);
    }

    @GetMapping
    public ModelAndView index() {

        List<Target> targetList = new ArrayList<>();
        targetList.add(Target.HOME_TARGET);
        targetList.add(new Target("/change-pw", "_CHANGE_PASSWORD"));
        targetList.add(new Target("/signout", "_SIGN_OUT"));

        EulerUserDetails userDetails = userContext.getUserDetails();
        List<String> authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (authorities.contains("root") || authorities.contains("admin")) {
            targetList.add(new Target("/admin/user", "用户管理"));
        }

        String welcome = ApplicationContextHolder.getApplicationContext().getMessage(
                "home.welcome",
                null,
                "Welcome!",
                this.getRequest().getLocale());

        return this.success(welcome + " " + userDetails.getUsername(), targetList.toArray(new Target[0]));
    }
}
