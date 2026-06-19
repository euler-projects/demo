package org.eulerframework.uc.controller;

import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Endpoints describing the currently authenticated user.
 *
 * <p>Mounted under both {@code /user} and {@code /api/user}; both refer
 * to the same singular resource (the caller).
 */
@RestController
@RequestMapping({"user", "api/user"})
public class UserController {
    private UserContext userContext;

    @GetMapping
    public Object whoami() {
        EulerUserDetails userDetails = userContext.getUserDetails();
        userDetails.eraseCredentials();
        return userDetails;
    }

    @GetMapping("authorities")
    public Collection<? extends GrantedAuthority> getUserAuthorities() {
        return userContext.getUserDetails().getAuthorities();
    }

    @Autowired
    public void setUserContext(UserContext userContext) {
        this.userContext = userContext;
    }
}
