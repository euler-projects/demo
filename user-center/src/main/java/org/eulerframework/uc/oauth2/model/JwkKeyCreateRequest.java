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
package org.eulerframework.uc.oauth2.model;

import org.eulerframework.uc.oauth2.service.jwk.keygen.JwkKeyGenAlgorithm;
import org.eulerframework.uc.oauth2.service.jwk.keygen.JwkKeyGenSpec;

/**
 * Admin-facing request DTO for the {@code POST /admin/oauth2/jwks} endpoint.
 * <p>
 * {@code algorithm} defaults to {@link JwkKeyGenAlgorithm#ES256} (EC P-256) when {@code null};
 * {@code keySize} is honoured only for RSA algorithms and is otherwise ignored. RSA without
 * an explicit {@code keySize} falls back to {@link JwkKeyGenSpec#DEFAULT_RSA_KEY_SIZE}.
 *
 * @param algorithm requested JWS algorithm
 * @param keySize   RSA key size in bits (2048 / 3072 / 4096); ignored for EC / EdDSA
 */
public record JwkKeyCreateRequest(JwkKeyGenAlgorithm algorithm, Integer keySize) {

    /** Default algorithm applied when {@link #algorithm()} is {@code null}. */
    public static final JwkKeyGenAlgorithm DEFAULT_ALGORITHM = JwkKeyGenAlgorithm.ES256;

    /** Resolve the request to an immutable {@link JwkKeyGenSpec} for the generator. */
    public JwkKeyGenSpec toSpec() {
        JwkKeyGenAlgorithm alg = this.algorithm == null ? DEFAULT_ALGORITHM : this.algorithm;
        Integer effectiveKeySize = alg.acceptsKeySize() ? this.keySize : null;
        return new JwkKeyGenSpec(alg, effectiveKeySize);
    }
}
