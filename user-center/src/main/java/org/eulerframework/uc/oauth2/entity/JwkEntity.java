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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingEntity;
import org.springframework.data.domain.Persistable;

/**
 * Persistent row of a JWK entry. The JWK itself is stored as AES-256-GCM
 * envelope ciphertext; {@code kid} is bound to the GCM tag as AAD by the
 * codec layer so row-level swaps are detected on decrypt.
 *
 * <p>Algorithm, use and {@code iat} are carried inside the encrypted JWK JSON;
 * the table only needs the lifecycle status alongside the cipher columns.
 */
@Entity
@Table(name = "oauth2_jwk")
public class JwkEntity extends AuditingEntity implements Persistable<String> {

    /** JWK kid (RFC 7517). Primary key. */
    @Id
    @Column(name = "kid", length = 128)
    private String kid;

    /** Lifecycle status. Stored as enum name. */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /** Identifier of the KEK used to encrypt this row. */
    @Column(name = "enc_kid", nullable = false, length = 64)
    private String encKid;

    /** 12-byte GCM IV. */
    @Column(name = "enc_iv", nullable = false, length = 16)
    private byte[] encIv;

    /** 16-byte GCM authentication tag. */
    @Column(name = "enc_tag", nullable = false, length = 16)
    private byte[] encTag;

    /** Ciphertext of the JWK JSON (private + public params). */
    @Column(name = "jwk_cipher", nullable = false)
    private byte[] jwkCipher;

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEncKid() {
        return encKid;
    }

    public void setEncKid(String encKid) {
        this.encKid = encKid;
    }

    public byte[] getEncIv() {
        return encIv;
    }

    public void setEncIv(byte[] encIv) {
        this.encIv = encIv;
    }

    public byte[] getEncTag() {
        return encTag;
    }

    public void setEncTag(byte[] encTag) {
        this.encTag = encTag;
    }

    public byte[] getJwkCipher() {
        return jwkCipher;
    }

    public void setJwkCipher(byte[] jwkCipher) {
        this.jwkCipher = jwkCipher;
    }

    @Override
    public String getId() {
        return this.kid;
    }

    @Override
    public boolean isNew() {
        return this.getCreatedDate() == null;
    }
}
