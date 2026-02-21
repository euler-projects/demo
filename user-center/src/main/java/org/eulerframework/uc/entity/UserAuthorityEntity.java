package org.eulerframework.uc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingEntity;
import org.eulerframework.data.entity.AuditingIdEntity;

@Entity
@Table(name = "t_user_authority")
public class UserAuthorityEntity extends AuditingIdEntity {
    @Column(name = "user_id")
    private String userId;
    @Column(name = "authority")
    private String authority;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}
