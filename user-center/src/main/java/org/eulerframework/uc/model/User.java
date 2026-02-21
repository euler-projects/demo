package org.eulerframework.uc.model;

import org.eulerframework.model.AbstractAuditingModel;
import org.eulerframework.security.core.EulerUser;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.uc.util.UserCenterModelUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class User extends AbstractAuditingModel implements EulerUser {
    private String userId;
    private String username;
    private String email;
    private String phone;
    private String password;
    private Collection<Authority> authorities;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;

    @Override
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Collection<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<Authority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void reloadUserDetails(EulerUserDetails userDetails) {
        this.setUserId(userDetails.getUserId());
        this.setUsername(userDetails.getUsername());
        this.setPassword(userDetails.getPassword());
        this.setAccountNonLocked(userDetails.isAccountNonLocked());
        this.setAccountNonExpired(userDetails.isAccountNonExpired());
        this.setCredentialsNonExpired(userDetails.isCredentialsNonExpired());
        this.setEnabled(userDetails.isEnabled());

        this.setAuthorities(Optional.ofNullable(userDetails.getAuthorities())
                .orElse(Collections.emptyList())
                .stream()
                .map(UserCenterModelUtils::toAuthority)
                .collect(Collectors.toList()));
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
