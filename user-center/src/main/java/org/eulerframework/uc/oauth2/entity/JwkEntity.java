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

package org.eulerframework.uc.oauth2.entity;

import com.nimbusds.jose.jwk.JWK;
import jakarta.persistence.*;
import org.eulerframework.data.entity.AuditingEntity;
import org.eulerframework.security.jwk.JwkStatus;
import org.eulerframework.uc.oauth2.entity.converter.JwkAttributeConverter;

import java.time.Instant;

@Entity
@Table(name = "oauth2_jwk")
public class JwkEntity extends AuditingEntity {

    @Id
    @Column(name = "kid", length = 36, nullable = false)
    private String kid;


    @Column(name = "iat", nullable = false)
    private Instant iat;

    @Column(name = "status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private JwkStatus status;

    @Column(name = "data", nullable = false)
    @Convert(converter = JwkAttributeConverter.class)
    private JWK data;

    @Column(name = "fingerprint", nullable = false)
    private byte[] fingerprint;

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public Instant getIat() {
        return iat;
    }

    public void setIat(Instant iat) {
        this.iat = iat;
    }

    public JwkStatus getStatus() {
        return status;
    }

    public void setStatus(JwkStatus status) {
        this.status = status;
    }

    public JWK getData() {
        return data;
    }

    public void setData(JWK data) {
        this.data = data;
    }

    public byte[] getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(byte[] fingerprint) {
        this.fingerprint = fingerprint;
    }
}
