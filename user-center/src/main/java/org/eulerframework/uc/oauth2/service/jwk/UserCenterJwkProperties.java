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
 * Data-encryption configuration for the JPA-backed JWK store. Each supported
 * algorithm has its own sub-block; {@link DataEncryption#getPrimaryAlg()}
 * selects which one is used for writes. Reads dispatch by the algorithm
 * identifier carried in each envelope header, so historical rows written
 * under a different algorithm remain decryptable as long as the corresponding
 * block is still enabled.
 *
 * <p>Property prefix: {@code euler.uc.oauth2.jwk}.
 */
@ConfigurationProperties(prefix = "euler.uc.oauth2.jwk")
public class UserCenterJwkProperties {

    /** Data-encryption sub-configuration. */
    private final DataEncryption dataEncryption = new DataEncryption();

    public DataEncryption getDataEncryption() {
        return dataEncryption;
    }

    /** Encryption algorithm configuration for the {@code oauth2_jwk.data} column. */
    public static class DataEncryption {

        /**
         * Identifier of the algorithm used for write operations. MUST match
         * the {@code algorithmId} of one of the enabled algorithm sub-blocks
         * (e.g. {@code AES-256-GCM} or {@code plain}).
         */
        private String primaryAlg;

        /** AES-256-GCM algorithm configuration. */
        private final AesGcm aesGcm = new AesGcm();

        /** Plaintext algorithm configuration (development / historical-data). */
        private final Plain plain = new Plain();

        public String getPrimaryAlg() {
            return primaryAlg;
        }

        public void setPrimaryAlg(String primaryAlg) {
            this.primaryAlg = primaryAlg;
        }

        public AesGcm getAesGcm() {
            return aesGcm;
        }

        public Plain getPlain() {
            return plain;
        }
    }

    /**
     * AES-256-GCM sub-block. {@link #keyFile} takes precedence; when it is
     * blank the cipher falls back to {@link #passphrase} (development only).
     * Both blank triggers a fail-fast at startup.
     */
    public static class AesGcm {

        /** Whether this algorithm is registered. */
        private boolean enabled;

        /**
         * Absolute file system path to a 32-byte binary KEK. When non-blank,
         * the cipher is built via the KEY_FILE source. POSIX permissions MUST
         * be {@code 0600} (owner read/write only).
         */
        private String keyFile;

        /**
         * Development-only passphrase used to derive the KEK via
         * PBKDF2-HMAC-SHA256 (600k iterations). Consulted only when
         * {@link #keyFile} is blank. Never set this in production.
         */
        private String passphrase;

        /**
         * Identifier of the KEK currently in effect, recorded in every
         * envelope header's {@code kid} field and required for AEAD.
         */
        private String kid;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }
    }

    /** Plaintext algorithm sub-block (unsafe; development / historical-data only). */
    public static class Plain {

        /** Whether this algorithm is registered. Defaults to {@code false}. */
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
