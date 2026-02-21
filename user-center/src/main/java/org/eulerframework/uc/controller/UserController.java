package org.eulerframework.uc.controller;

import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping({"user", "api/user"})
public class UserController {
    private UserContext userContext;

    @GetMapping(path = {"", "whoami"})
    public Object whoami() {
        EulerUserDetails userDetails = userContext.getUserDetails();
        userDetails.eraseCredentials();
        return userDetails;
    }

    @GetMapping(path = {"authority", "authorities"})
    public Collection<? extends GrantedAuthority> getUserAuthority() {
        return userContext.getUserDetails().getAuthorities();
    }

    @Autowired
    public void setUserContext(UserContext userContext) {
        this.userContext = userContext;
    }
}
