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

import org.eulerframework.security.jwk.JwkStatus;

/**
 * Admin-facing request DTO for {@code PUT /admin/oauth2/jwks/{kid}} and
 * {@code PATCH /admin/oauth2/jwks/{kid}}.
 *
 * <p>Only {@link JwkStatus status} is exposed: a JWK is immutable by {@code kid}
 * (changing key material would require issuing a new {@code kid}), so every
 * mutable field boils down to the lifecycle state.
 *
 * <p>Semantics differ between PUT and PATCH:
 * <ul>
 *   <li>{@code PUT}: {@code status} is mandatory; the controller rejects a
 *       {@code null} value with {@code 400 Bad Request}.</li>
 *   <li>{@code PATCH}: {@code status} may be {@code null}, meaning "leave
 *       the persisted status unchanged".</li>
 * </ul>
 *
 * @param status the requested lifecycle state
 */
public record JwkKeyPatchRequest(JwkStatus status) {
}
