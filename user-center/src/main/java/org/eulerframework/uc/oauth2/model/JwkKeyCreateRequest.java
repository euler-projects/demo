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


public record JwkKeyCreateRequest(JwkKeyGenAlgorithm algorithm, Integer keySize) {

    /**
     * Default algorithm applied when {@link #algorithm()} is {@code null}.
     */
    public static final JwkKeyGenAlgorithm DEFAULT_ALGORITHM = JwkKeyGenAlgorithm.ES256;

    /**
     * Resolve the request to an immutable {@link JwkKeyGenSpec} for the generator.
     */
    public JwkKeyGenSpec toSpec() {
        return new JwkKeyGenSpec(this.algorithm == null ? DEFAULT_ALGORITHM : this.algorithm, this.keySize);
    }
}
