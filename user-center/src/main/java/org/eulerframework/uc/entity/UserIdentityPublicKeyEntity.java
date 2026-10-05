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
 * Child row carrying the public key of a {@code public_key} user identity.
 *
 * <p>The cross-account uniqueness key &mdash; the RFC 7638 JWK Thumbprint of the key
 * &mdash; lives on the parent identity row's {@code subject} column, so this table holds
 * only the key material itself. Exactly one key per identity: there is no rotation, and a
 * caller who loses the private half registers a new key and opens a new account rather
 * than replacing this one.
 *
 * <p>The key is public, so unlike the phone and email child rows it is stored in clear;
 * the column is what the login flow verifies an assertion signature against and what the
 * {@code identities} projection hands back to the client.
 */
@Entity
@Table(name = "t_user_identity_public_key")
public class UserIdentityPublicKeyEntity extends AuditingEntity {

    @Id
    @Column(name = "identity_id", length = 36)
    private String identityId;

    /**
     * The registered public key as a JWK (RFC 7517) in JSON. Always carries a {@code kid}
     * equal to the parent row's {@code subject}, so a login can select among an account's
     * keys by the {@code kid} its assertion header names.
     *
     * <p>The column is TEXT and the length here mirrors it rather than bounding it: what
     * gets stored is the key the issued-key registry already accepted, and that endpoint
     * caps its request body far below TEXT. No length is enforced in the service, so this
     * mapping is the only place the schema's shape is restated in code &mdash; keep the two
     * in step. See {@code V008__add_public_key_identity.sql} for why TEXT and not JSON.
     */
    @Column(name = "jwk", nullable = false, length = 65535)
    private String jwk;

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public String getJwk() {
        return jwk;
    }

    public void setJwk(String jwk) {
        this.jwk = jwk;
    }
}
