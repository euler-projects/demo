package org.eulerframework.uc.controller.admin;

import org.eulerframework.security.core.EulerAuthority;
import org.eulerframework.security.core.EulerUser;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.util.UserDetailsUtils;
import org.eulerframework.uc.model.User;
import org.eulerframework.uc.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/user")
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

    @GetMapping("list")
    public List<EulerUser> listUsers(
            @RequestParam int offset,
            @RequestParam int limit
    ) {
        return this.userService.listUsers(offset, limit)
                .stream()
                .peek(CredentialsContainer::eraseCredentials)
                .toList();
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        this.userService.updateUser(user);
        User updatedUser = this.userService.loadUserById(user.getUserId());
        updatedUser.eraseCredentials();
        return updatedUser;
    }

    @DeleteMapping
    public void deleteUser(@RequestParam String userId) {
        this.userService.deleteUser(userId);
    }

    @PostMapping("reset-password")
    public void resetPassword(@RequestParam String userId, @RequestParam String password) {
        User loadedUser = this.userService.loadUserById(userId);
        UserDetails userDetails = UserDetailsUtils.toEulerUserDetails(loadedUser);
        this.userDetailsPasswordService.updatePassword(userDetails, this.passwordEncoder.encode(password));
    }
}
