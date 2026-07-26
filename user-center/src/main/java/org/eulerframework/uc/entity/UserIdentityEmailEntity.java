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
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingEntity;
import org.eulerframework.uc.entity.converter.EmailAttributeConverter;

/**
 * Child row carrying the email-specific PII of a user identity.
 * <p>
 * The cross-account uniqueness key (SHA-256 hex of the normalised email)
 * lives on the parent identity row's {@code subject} column &mdash; this
 * child table holds only the encrypted original address used for
 * email-OTP delivery and masked-display projection.
 */
@Entity
@Table(name = "t_user_identity_email")
public class UserIdentityEmailEntity extends AuditingEntity {

    @Id
    @Column(name = "identity_id", length = 36)
    private String identityId;

    @Column(name = "email", nullable = false, length = 1024)
    @Convert(converter = EmailAttributeConverter.class)
    private String email;

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
}
