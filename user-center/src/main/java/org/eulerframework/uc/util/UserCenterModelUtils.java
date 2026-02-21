package org.eulerframework.uc.util;

import org.eulerframework.data.util.AuditingEntityUtils;
import org.eulerframework.security.core.EulerGrantedAuthority;
import org.eulerframework.uc.entity.AuthorityEntity;
import org.eulerframework.uc.entity.UserEntity;
import org.eulerframework.uc.model.Authority;
import org.eulerframework.uc.model.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

public abstract class UserCenterModelUtils {
    public static User toUser(UserEntity entity, Collection<AuthorityEntity> authorityEntities) {
        if (entity == null) {
            return null;
        }

        User model = new User();

        model.setUserId(entity.getUserId());
        model.setUsername(entity.getUsername());
        model.setEmail(entity.getEmail());
        model.setPhone(entity.getPhone());
        model.setPassword(entity.getPassword());

        model.setAccountNonExpired(Optional.ofNullable(entity.getAccountNonExpired()).orElse(false));
        model.setAccountNonLocked(Optional.ofNullable(entity.getAccountNonLocked()).orElse(false));
        model.setCredentialsNonExpired(Optional.ofNullable(entity.getCredentialsNonExpired()).orElse(false));
        model.setEnabled(Optional.ofNullable(entity.getEnabled()).orElse(false));

        model.setAuthorities(Optional.ofNullable(authorityEntities)
                .orElse(Collections.emptyList())
                .stream()
                .map(UserCenterModelUtils::toAuthority)
                .collect(Collectors.toList()));

        return model;
    }

    public static UserEntity toUserEntity(User model) {
        if (model == null) {
            return null;
        }

        UserEntity entity = new UserEntity();

        entity.setUserId(model.getUserId());
        entity.setUsername(model.getUsername());
        entity.setEmail(model.getEmail());
        entity.setPhone(model.getPhone());
        entity.setPassword(model.getPassword());

        entity.setAccountNonExpired(model.isAccountNonExpired());
        entity.setAccountNonLocked(model.isAccountNonLocked());
        entity.setCredentialsNonExpired(model.isCredentialsNonExpired());
        entity.setEnabled(model.isEnabled());

        AuditingEntityUtils.updateAuditingEntity(model, entity);

        return entity;
    }

    public static AuthorityEntity toAuthorityEntity(Authority model) {
        if (model == null) {
            return null;
        }

        AuthorityEntity entity = new AuthorityEntity();
        entity.setAuthority(model.getAuthority());
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());

        AuditingEntityUtils.updateAuditingEntity(model, entity);

        return entity;
    }

    public static Authority toAuthority(AuthorityEntity entity) {
        if (entity == null) {
            return null;
        }

        Authority model = new Authority();
        model.setAuthority(entity.getAuthority());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        return model;
    }

    public static Authority toAuthority(GrantedAuthority grantedAuthority) {
        if (grantedAuthority == null) {
            return null;
        }

        Authority authority = new Authority();
        if (grantedAuthority instanceof EulerGrantedAuthority eulerGrantedAuthority) {
            authority.setAuthority(eulerGrantedAuthority.getAuthority());
            authority.setName(eulerGrantedAuthority.getName());
            authority.setDescription(eulerGrantedAuthority.getDescription());
        } else {
            authority.setAuthority(grantedAuthority.getAuthority());
        }
        return authority;
    }
}
