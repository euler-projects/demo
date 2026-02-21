package org.eulerframework.uc.service;

import jakarta.annotation.Resource;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.core.userdetails.provider.LocalEulerUserDetailsProvider;
import org.eulerframework.security.util.UserDetailsUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(2)
public class PhoneUserDetailsProvider implements LocalEulerUserDetailsProvider {
    @Resource
    private UserService userService;

    @Override
    public EulerUserDetails provide(String principal) {
        return UserDetailsUtils.toEulerUserDetails(this.userService.loadUserByPhone(principal));
    }
}
