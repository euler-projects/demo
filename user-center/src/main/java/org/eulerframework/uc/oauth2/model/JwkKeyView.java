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

import org.eulerframework.security.oauth2.server.authorization.jwk.JwkEntry;
import org.eulerframework.security.oauth2.server.authorization.jwk.JwkStatus;

import java.time.Instant;

/**
 * Admin-friendly projection of a published JWK entry. Never includes private-key
 * material; {@link #hasPrivate()} is exposed only to help the UI annotate rows that
 * can be promoted to signing via the admin {@code promote} flow.
 *
 * @param kid        key id
 * @param alg        JWA algorithm name (RS256 / ES256 / EdDSA / ...)
 * @param use        JWK use parameter (typically {@code sig})
 * @param status     lifecycle state of the entry
 * @param hasPrivate whether a private key is present at the authoritative repository
 * @param issuedAt   issuance timestamp declared in the manifest
 * @param signing    whether this entry is currently selected as the signing key
 */
public record JwkKeyView(
        String kid,
        String alg,
        String use,
        JwkStatus status,
        boolean hasPrivate,
        Instant issuedAt,
        boolean signing) {

    public static JwkKeyView from(JwkEntry entry, boolean signing) {
        return new JwkKeyView(
                entry.kid(),
                entry.jwk().getAlgorithm() == null ? null : entry.jwk().getAlgorithm().getName(),
                entry.jwk().getKeyUse() == null ? null : entry.jwk().getKeyUse().identifier(),
                entry.status(),
                entry.hasPrivateKey(),
                entry.issuedAt(),
                signing);
    }
}
