package org.eulerframework.uc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingEntity;

@Entity
@Table(name = "t_device_user_mapping")
public class DeviceUserMappingEntity extends AuditingEntity {
    @Id
    @Column(name = "key_id")
    private String keyId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "team_id", nullable = false)
    private String teamId;

    @Column(name = "bundle_id", nullable = false)
    private String bundleId;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }
}
