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

package org.eulerframework.uc.oauth2.service.jwk;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KEK (Key Encryption Key) configuration for the JPA-backed JWK store.
 * Controls how {@link JwkKekCodec} acquires its AES-256 master key used to
 * envelope-encrypt every JWK row in {@code oauth2_jwk}.
 *
 * <p>Property prefix: {@code euler.uc.oauth2.jwk.encryption}.
 */
@ConfigurationProperties(prefix = "euler.uc.oauth2.jwk.encryption")
public class UserCenterJwkProperties {

    /**
     * Identifier recorded in the {@code enc_kid} column with every encrypted
     * row. MUST be changed together with the KEK itself on rotation (KEK
     * rotation is otherwise unsupported by the current codec). Required.
     */
    private String encKid;

    /** KEK source selection. Defaults to {@link Source#PASSPHRASE}. */
    private Source source = Source.PASSPHRASE;

    /**
     * Absolute file system path to a 32-byte binary KEK. Required when
     * {@link #source} is {@link Source#KEY_FILE}. POSIX permissions MUST be
     * {@code 0600} (owner read/write only).
     */
    private String keyFile;

    /**
     * Development-only passphrase used to derive the KEK via PBKDF2-HMAC-SHA256
     * (600k iterations). Required when {@link #source} is
     * {@link Source#PASSPHRASE}. Never set this in production.
     */
    private String passphrase;

    public String getEncKid() {
        return encKid;
    }

    public void setEncKid(String encKid) {
        this.encKid = encKid;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /** KEK material source. */
    public enum Source {
        /** Load a 32-byte binary KEK from a file on disk. Production-recommended. */
        KEY_FILE,
        /** Derive the KEK from a passphrase via PBKDF2. Development only. */
        PASSPHRASE
    }
}
