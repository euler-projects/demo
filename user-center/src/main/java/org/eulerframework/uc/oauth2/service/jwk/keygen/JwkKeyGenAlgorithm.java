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

/**
 * JWS signing algorithms supported by the server-side {@link JwkGenerator}.
 * The enum value maps directly to the JOSE {@code alg} header carried by the
 * JWS tokens signed with the generated key.
 *
 * <ul>
 *   <li>RSA ({@code RS256/RS384/RS512}) honours the {@code keySize} option
 *       (2048 / 3072 / 4096 bits) supplied via {@link JwkKeyGenSpec}.</li>
 *   <li>EC ({@code ES256/ES384/ES512}) is bound to the canonical curve
 *       (P-256 / P-384 / P-521); {@code keySize} is ignored.</li>
 *   <li>{@code EDDSA} is bound to Ed25519 and requires BouncyCastle on the
 *       classpath; {@code keySize} is ignored.</li>
 * </ul>
 *
 * <p>This enum is intentionally scoped to the key-generation concern of the
 * user-center application layer; the framework-level declarative enum for
 * pre-configured PEM entries ({@code JwkPemAlgorithm}) is a separate type and
 * deliberately omits Ed25519.
 */
public enum JwkKeyGenAlgorithm {

    RS256, RS384, RS512,
    ES256, ES384, ES512,
    EDDSA;

    /**
     * @return JOSE algorithm name: {@code RS*} / {@code ES*} are returned verbatim;
     *         {@code EDDSA} is mapped to the canonical {@code "EdDSA"}.
     */
    public String joseName() {
        return this == EDDSA ? "EdDSA" : name();
    }

    /** Whether the {@code keySize} option in {@link JwkKeyGenSpec} is honoured. */
    public boolean acceptsKeySize() {
        return name().startsWith("RS");
    }
}
