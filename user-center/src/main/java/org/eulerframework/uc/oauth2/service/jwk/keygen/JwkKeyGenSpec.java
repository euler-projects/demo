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
package org.eulerframework.uc.oauth2.service.jwk.keygen;

import java.util.Objects;

/**
 * Immutable parameter bundle for a single key generation request.
 * <p>
 * {@link #keySize()} is meaningful only for RSA algorithms; for EC and EdDSA the
 * curve is implied by {@link #algorithm()} and any explicit value is ignored.
 *
 * @param algorithm target JWS algorithm
 * @param keySize   RSA key size in bits (2048 / 3072 / 4096); ignored for EC / EdDSA
 */
public record JwkKeyGenSpec(JwkKeyGenAlgorithm algorithm, Integer keySize) {

    /** Default RSA key size when the caller omits {@link #keySize()}. */
    public static final int DEFAULT_RSA_KEY_SIZE = 2048;

    public JwkKeyGenSpec {
        Objects.requireNonNull(algorithm, "algorithm");
        if (algorithm.acceptsKeySize() && keySize != null) {
            switch (keySize) {
                case 2048, 3072, 4096 -> { /* allowed */ }
                default -> throw new IllegalArgumentException(
                        "RSA keySize must be one of 2048/3072/4096, got " + keySize);
            }
        }
    }

    /** Convenience factory: RSA with the specified key size. */
    public static JwkKeyGenSpec rsa(JwkKeyGenAlgorithm alg, int keySize) {
        if (!alg.acceptsKeySize()) {
            throw new IllegalArgumentException("Algorithm " + alg + " is not an RSA algorithm");
        }
        return new JwkKeyGenSpec(alg, keySize);
    }

    /** Convenience factory: EC / EdDSA, no key size. */
    public static JwkKeyGenSpec curve(JwkKeyGenAlgorithm alg) {
        if (alg.acceptsKeySize()) {
            throw new IllegalArgumentException("Algorithm " + alg + " requires a keySize");
        }
        return new JwkKeyGenSpec(alg, null);
    }

    /** Effective key size: callers' value when present, otherwise {@link #DEFAULT_RSA_KEY_SIZE}. */
    public int effectiveRsaKeySize() {
        if (!algorithm.acceptsKeySize()) {
            throw new IllegalStateException("Algorithm " + algorithm + " has no keySize");
        }
        return keySize == null ? DEFAULT_RSA_KEY_SIZE : keySize;
    }
}
