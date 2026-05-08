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

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.KeyUse;
import org.eulerframework.security.jwk.JwkStatus;
import org.eulerframework.security.jwk.ManagedJwk;

import java.time.Instant;


/**
 * Read-only admin-API projection of a managed JWK. It omits the key material
 * itself and exposes only metadata safe to return from management endpoints.
 *
 * @param kid        JWK {@code kid} claim
 * @param alg        JWK algorithm
 * @param use        JWK intended key use
 * @param iat        JWK issue time
 * @param status     lifecycle status
 * @param hasPrivate {@code true} when the underlying JWK carries a private part
 */
public record JwkKeyView(
        String kid,
        Algorithm alg,
        KeyUse use,
        Instant iat,
        JwkStatus status,
        boolean hasPrivate) {

    /**
     * Project a {@link ManagedJwk} into its admin-facing view.
     *
     * @param managedJwk source JWK; must carry a non-{@code null} {@link ManagedJwk#getJwk()}
     * @return view suitable for JSON serialisation
     */
    public static JwkKeyView from(ManagedJwk managedJwk) {
        return new JwkKeyView(
                managedJwk.getKid(),
                managedJwk.getJwk().getAlgorithm(),
                managedJwk.getJwk().getKeyUse(),
                managedJwk.getJwk().getIssueTime().toInstant(),
                managedJwk.getStatus(),
                managedJwk.getJwk().isPrivate());
    }
}
