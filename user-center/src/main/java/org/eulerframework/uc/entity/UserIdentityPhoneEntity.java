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
import org.eulerframework.uc.entity.converter.PhoneNumberAttributeConverter;

/**
 * Child row carrying the phone-specific PII of a user identity.
 * <p>
 * The cross-account uniqueness key (SHA-256 hex of the phone) lives on
 * the parent identity row's {@code subject} column &mdash; this child
 * table holds only the encrypted E.164 original used for SMS-OTP
 * delivery and masked-display projection. Persisting the encrypted PII
 * once on the identity aggregate (rather than once per authentication
 * factor that targets the same phone) lets future factors share it
 * without duplication.
 */
@Entity
@Table(name = "t_user_identity_phone")
public class UserIdentityPhoneEntity extends AuditingEntity {

    @Id
    @Column(name = "identity_id", length = 36)
    private String identityId;

    @Column(name = "phone", nullable = false, length = 512)
    @Convert(converter = PhoneNumberAttributeConverter.class)
    private String phone;

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
