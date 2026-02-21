package org.eulerframework.uc.model;

import org.eulerframework.model.AbstractAuditingModel;
import org.eulerframework.security.core.EulerAuthority;

public class Authority extends AbstractAuditingModel implements EulerAuthority {
    private String authority;
    private String name;
    private String description;

    @Override
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
