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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingUUIDEntity;

import java.time.Instant;

/**
 * Parent row of a user identity (用户身份) bound to a user.
 * <p>
 * Holds the universal identity envelope:
 * <ul>
 *     <li>{@code identity_id} &mdash; opaque UUID primary key, used as
 *         the REST resource identifier on {@code /user/identities/{id}}.
 *         Stays stable across rebinds.</li>
 *     <li>{@code subject} &mdash; the deterministic per-type unique key
 *         of the underlying identity. For {@code phone} / {@code email}
 *         this is the SHA-256 hex of the lower-cased / E.164-normalised
 *         original; for {@code wechat} / {@code apple} / {@code google}
 *         this is the IdP-issued opaque {@code openid} / {@code sub}.
 *         Indexed together with {@code identity_type} for cross-account
 *         uniqueness and consumed by the OAuth2 OTP grant for reverse
 *         lookups. Surfaced on the public API as {@code subject}.</li>
 * </ul>
 * Identity-type-specific PII (the encrypted E.164 phone, the WeChat
 * profile cache, ...) lives in dedicated child tables joined on
 * {@code identity_id}. The framework deliberately avoids putting any
 * type-shaped column on this parent table.
 */
@Entity
@Table(name = "t_user_identity")
@AttributeOverride(name = "id", column = @Column(name = "identity_id", length = 36))
public class UserIdentityEntity extends AuditingUUIDEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "identity_type", nullable = false, length = 32)
    private String identityType;

    @Column(name = "subject", nullable = false, length = 128)
    private String subject;

    @Column(name = "bound_at", nullable = false)
    private Instant boundAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getBoundAt() {
        return boundAt;
    }

    public void setBoundAt(Instant boundAt) {
        this.boundAt = boundAt;
    }
}
