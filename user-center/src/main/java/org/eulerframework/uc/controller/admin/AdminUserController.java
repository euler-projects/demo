package org.eulerframework.uc.controller.admin;

import org.eulerframework.security.core.EulerAuthority;
import org.eulerframework.security.core.EulerUser;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.util.UserDetailsUtils;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.model.UserPasswordResetRequest;
import org.eulerframework.uc.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-facing CRUD endpoints for users.
 */
@RestController
@RequestMapping("admin/api/users")
@PreAuthorize("hasAnyAuthority('root', 'admin')")
public class AdminUserController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsPasswordService userDetailsPasswordService;

    public AdminUserController(UserService userService, PasswordEncoder passwordEncoder, UserDetailsPasswordService userDetailsPasswordService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsPasswordService = userDetailsPasswordService;
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        // 通过EulerUserDetails规范化敏感字段
        EulerUserDetails userDetails = EulerUserDetails.builder()
                .passwordEncoder(this.passwordEncoder::encode)
                .username(user.getUsername())
                .password(user.getPassword())
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .authorities(user.getAuthorities().stream().map(EulerAuthority::getAuthority).toArray(String[]::new))
                .build();
        User userCreation = new User();
        userCreation.reloadUserDetails(userDetails);
        userCreation.setPhone(user.getPhone());
        userCreation.setEmail(user.getEmail());
        User creaetedUser = this.userService.createUser(userCreation);
        creaetedUser.eraseCredentials();
        return creaetedUser;
    }

    @GetMapping
    public List<EulerUser> listUsers(
            @RequestParam int offset,
            @RequestParam int limit
    ) {
        return this.userService.listUsers(offset, limit)
                .stream()
                .peek(CredentialsContainer::eraseCredentials)
                .toList();
    }

    @PutMapping("/{userId}")
    public User updateUser(@PathVariable String userId, @RequestBody User user) {
        user.setUserId(userId);
        this.userService.updateUser(user);
        User updatedUser = this.userService.loadUserById(userId);
        updatedUser.eraseCredentials();
        return updatedUser;
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String userId) {
        this.userService.deleteUser(userId);
    }

    /**
     * Replaces the user's password.
     */
    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable String userId,
                              @RequestBody UserPasswordResetRequest request) {
        User loadedUser = this.userService.loadUserById(userId);
        UserDetails userDetails = UserDetailsUtils.toEulerUserDetails(loadedUser);
        this.userDetailsPasswordService.updatePassword(userDetails,
                this.passwordEncoder.encode(request.password()));
    }
}
