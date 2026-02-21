package org.eulerframework.uc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingEntity;

@Entity
@Table(name = "t_authority")
public class AuthorityEntity extends AuditingEntity {
    @Id
    @Column(name = "authority")
    private String authority;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
