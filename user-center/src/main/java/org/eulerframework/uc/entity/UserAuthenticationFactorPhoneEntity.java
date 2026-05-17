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
 * Child row carrying the phone-specific state of an authentication factor.
 * The {@code phone} column persists the raw E.164 phone number transparently
 * encrypted via {@link PhoneNumberAttributeConverter}; only the framework
 * factor identifier (SHA-256 of the phone number) is exposed for uniqueness
 * checks at the {@code t_user_authentication_factor.identifier} column.
 */
@Entity
@Table(name = "t_user_authentication_factor_phone")
public class UserAuthenticationFactorPhoneEntity extends AuditingEntity {

    @Id
    @Column(name = "factor_id", length = 36)
    private String factorId;

    @Column(name = "phone", nullable = false, columnDefinition = "text")
    @Convert(converter = PhoneNumberAttributeConverter.class)
    private String phone;

    public String getFactorId() {
        return factorId;
    }

    public void setFactorId(String factorId) {
        this.factorId = factorId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
