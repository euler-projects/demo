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

import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;

/**
 * One-shot registration response returned by {@code POST} on the Admin
 * OAuth 2.0 Client API.
 *
 * <p>Extends {@link OAuth2ClientResponse} to surface every attribute of the
 * freshly created client plus the server-minted plaintext
 * {@code clientSecret}. Operators must capture the secret on this single
 * response: subsequent {@code GET} / {@code LIST} endpoints only ever return
 * the opaque encoded form stored on disk, and the plaintext is not retained
 * anywhere on the server.
 *
 * <p>The {@code clientSecret} field is {@code null} for clients whose
 * {@code token_endpoint_auth_method} does not require a shared secret
 * (e.g. {@code none}, {@code private_key_jwt}, {@code tls_client_auth},
 * {@code self_signed_tls_client_auth}).
 */
public class OAuth2ClientCreatedResponse extends OAuth2ClientResponse {

    private String clientSecret;

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Projects a freshly created {@link EulerOAuth2Client} into its one-shot
     * transport form, attaching the plaintext secret when one was minted.
     *
     * @param model           the created client, or {@code null}
     * @param plaintextSecret the freshly generated plaintext secret, or
     *                        {@code null} when the client uses a non-secret
     *                        authentication method
     * @return the DTO, or {@code null} when the source model is {@code null}
     */
    public static OAuth2ClientCreatedResponse of(EulerOAuth2Client model, String plaintextSecret) {
        if (model == null) {
            return null;
        }
        OAuth2ClientCreatedResponse dto = new OAuth2ClientCreatedResponse();
        populateResponse(dto, model);
        dto.clientSecret = plaintextSecret;
        return dto;
    }
}
