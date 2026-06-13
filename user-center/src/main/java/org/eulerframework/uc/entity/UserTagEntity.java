package org.eulerframework.uc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingIdEntity;

@Entity
@Table(name = "t_user_tag")
public class UserTagEntity extends AuditingIdEntity {
    @Column(name = "user_id")
    private String userId;
    @Column(name = "tag_k")
    private String tagKey;
    @Column(name = "tag_v")
    private String tagValue;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }
}
