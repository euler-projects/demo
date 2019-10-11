package org.eulerframework.uc.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.eulerframework.web.core.annotation.JspController;
import org.eulerframework.web.core.base.controller.JspSupportWebController;

@JspController("designUserWebController")
@RequestMapping("/")
public class UserJspController extends JspSupportWebController {
    
//    @Resource(name="org.springframework.security.authenticationManager")
//    //@Qualifier("org.springframework.security.authenticationManager")//编辑软件会提示错误
//    private AuthenticationManager authenticationManager;
//    
//    private void autoSignin(String username, String password) {
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
//                username, password);
//
//        HttpServletRequest request = this.getRequest();
//        //request.getSession();
//
//        token.setDetails(new WebAuthenticationDetails(request));
//        Authentication authenticatedUser = authenticationManager
//                .authenticate(token);
//   
//        SecurityContextHolder.getContext().setAuthentication(authenticatedUser);
//        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
//    }

}
