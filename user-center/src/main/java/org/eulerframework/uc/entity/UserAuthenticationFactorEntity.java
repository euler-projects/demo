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

import java.time.Instant;

/**
 * Parent row of an authentication factor (登录因素) bound to a user.
 * Factor-specific state lives in dedicated child tables, e.g.
 * {@link UserAuthenticationFactorPhoneEntity}.
 */
@Entity
@Table(name = "t_user_authentication_factor")
public class UserAuthenticationFactorEntity extends AuditingEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "factor_type", nullable = false)
    private String factorType;

    @Column(name = "identifier", nullable = false)
    private String identifier;

    @Column(name = "bound_at", nullable = false)
    private Instant boundAt;

    @Column(name = "last_verified_at", nullable = false)
    private Instant lastVerifiedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFactorType() {
        return factorType;
    }

    public void setFactorType(String factorType) {
        this.factorType = factorType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Instant getBoundAt() {
        return boundAt;
    }

    public void setBoundAt(Instant boundAt) {
        this.boundAt = boundAt;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }
}
