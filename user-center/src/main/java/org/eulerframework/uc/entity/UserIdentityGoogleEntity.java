/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.uc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingEntity;

/**
 * Child row carrying the Google-specific profile snapshot of a user
 * identity. The cross-account uniqueness key (the raw Google
 * {@code sub}) lives on the parent identity row's {@code subject}
 * column - this child table holds only the additional profile
 * attributes returned by Google's UserInfo endpoint, so that the
 * profile can be projected without having to re-hit Google on every
 * lookup.
 *
 * <p>The stored fields are refreshed only on first bind; keeping them
 * eventually consistent with Google is not a requirement of the
 * identity SPI and is intentionally out of scope for this table.
 */
@Entity
@Table(name = "t_user_identity_google")
public class UserIdentityGoogleEntity extends AuditingEntity {

    @Id
    @Column(name = "identity_id", length = 36)
    private String identityId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "picture", length = 1024)
    private String picture;

    @Column(name = "locale", length = 32)
    private String locale;

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
