package net.eulerframework.web.module.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import net.eulerframework.web.core.annotation.WebController;
import net.eulerframework.web.core.base.controller.JspSupportWebController;

@WebController("designUserWebController")
@RequestMapping("/")
public class UserWebController extends JspSupportWebController {
    
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
