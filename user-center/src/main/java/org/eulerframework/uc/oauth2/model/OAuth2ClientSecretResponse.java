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

import java.time.Instant;

/**
 * One-shot response carrying a freshly minted client secret.
 *
 * <p>Returned by the client-secret rotation endpoint; the plaintext is shown
 * to the operator exactly once and never persisted in plaintext form. The
 * {@code clientSecretExpiresAt} attribute echoes the value recorded on the
 * client; a {@code null} value means "never expires", matching the default
 * rotation policy.
 *
 * @param clientSecret          the freshly generated plaintext secret
 * @param clientSecretExpiresAt expiry instant, or {@code null} for "never"
 */
public record OAuth2ClientSecretResponse(String clientSecret, Instant clientSecretExpiresAt) {
}
