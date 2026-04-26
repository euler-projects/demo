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

package org.eulerframework.uc.oauth2.controller.admin;

import org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2ClientService;
import org.eulerframework.uc.oauth2.model.OAuth2ClientCreatedResponse;
import org.eulerframework.uc.oauth2.model.OAuth2ClientRequest;
import org.eulerframework.uc.oauth2.model.OAuth2ClientResponse;
import org.eulerframework.uc.oauth2.model.OAuth2ClientSecretResponse;
import org.eulerframework.uc.oauth2.util.OAuth2ClientSecretGenerator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-facing CRUD endpoints for OAuth 2.0 clients.
 *
 * <p>Requests are modelled by {@link OAuth2ClientRequest} and responses by
 * {@link OAuth2ClientResponse}, which intentionally diverge in their field
 * sets: server-owned, read-only attributes such as {@code registrationId},
 * {@code clientId} and {@code clientIdIssuedAt} are emitted on the response
 * but rejected on the request per
 * <a href="https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.1">
 * RFC&nbsp;7591 §3.2.1</a>. The split also lets the domain
 * {@link DefaultEulerOAuth2Client} stay free of JSON concerns &mdash; its
 * {@code AbstractSettings}-derived blocks do not round-trip cleanly through
 * Jackson, whereas the DTOs flatten those blocks into JSON-native fields and
 * perform the conversion explicitly.
 *
 * <p>The {@code clientSecret} is owned end-to-end by the server:
 * <ul>
 *   <li>On {@code POST}, a cryptographically strong plaintext secret is
 *       minted by {@link OAuth2ClientSecretGenerator}, hashed via the
 *       configured {@link PasswordEncoder} before persistence, and returned
 *       exactly once through {@link OAuth2ClientCreatedResponse}. No secret
 *       is minted when {@code token_endpoint_auth_method} does not use a
 *       shared secret (e.g. {@code none}, {@code private_key_jwt},
 *       {@code tls_client_auth}).</li>
 *   <li>On subsequent {@code GET} / {@code LIST} responses the credential is
 *       omitted &mdash; what the service persists is the hash, which would
 *       only leak the on-disk representation if disclosed.</li>
 *   <li>{@link #rotateClientSecret(String)} mints a fresh plaintext secret
 *       and returns it through a dedicated one-shot
 *       {@link OAuth2ClientSecretResponse}.</li>
 * </ul>
 *
 * <p>{@code clientSecretExpiresAt} defaults to {@code null} ("never");
 * enforcing a finite lifetime is a future policy concern, handled by the
 * same generator/rotation pipeline.
 */
@RestController
@RequestMapping(path = {"admin/oauth2/client", "api/admin/oauth2/client"})
@PreAuthorize("hasAnyAuthority('root', 'admin')")
public class AdminOAuth2ClientController {

    private final EulerOAuth2ClientService oauth2ClientService;
    private final PasswordEncoder passwordEncoder;

    public AdminOAuth2ClientController(EulerOAuth2ClientService oauth2ClientService,
                                       PasswordEncoder passwordEncoder) {
        this.oauth2ClientService = oauth2ClientService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new OAuth 2.0 client.
     *
     * <p>When the requested {@code token_endpoint_auth_method} relies on a
     * shared secret, a cryptographically strong plaintext secret is minted
     * server-side, hashed with the configured {@link PasswordEncoder} before
     * persistence, and returned <em>once</em> on the response so the
     * operator can capture it. Otherwise the {@code clientSecret} attribute
     * of the response is {@code null}.
     *
     * <p>{@code clientSecretExpiresAt} defaults to {@code null} ("never");
     * operators rotate the secret explicitly via
     * {@link #rotateClientSecret(String)}.
     *
     * @param request the client to create
     * @return the created client, with the one-shot plaintext secret when applicable
     */
    @PostMapping
    public OAuth2ClientCreatedResponse createClient(@RequestBody OAuth2ClientRequest request) {
        DefaultEulerOAuth2Client model = request.toModel();
        String plaintextSecret = null;
        if (OAuth2ClientSecretGenerator.requiresClientSecret(model.getTokenEndpointAuthMethod())) {
            plaintextSecret = OAuth2ClientSecretGenerator.generate();
            model.setClientSecret(this.passwordEncoder.encode(plaintextSecret));
        } else {
            model.setClientSecret(null);
        }
        model.setClientSecretExpiresAt(null);
        EulerOAuth2Client created = this.oauth2ClientService.createClient(model);
        return OAuth2ClientCreatedResponse.of(created, plaintextSecret);
    }

    /**
     * Retrieves a client by its registration ID.
     *
     * @param registrationId the registration identifier
     * @return the client, or {@code null} if not found
     */
    @GetMapping("/{registrationId}")
    public OAuth2ClientResponse getClient(@PathVariable String registrationId) {
        EulerOAuth2Client client = this.oauth2ClientService.loadClientByRegistrationId(registrationId);
        return OAuth2ClientResponse.fromModel(client);
    }

    /**
     * Lists clients with pagination.
     *
     * @param offset the offset of the first result
     * @param limit  the maximum number of results
     * @return a list of clients
     */
    @GetMapping
    public List<OAuth2ClientResponse> listClients(
            @RequestParam int offset,
            @RequestParam int limit
    ) {
        return this.oauth2ClientService.listClients(offset, limit)
                .stream()
                .map(OAuth2ClientResponse::fromModel)
                .toList();
    }

    /**
     * Updates an existing client using patch semantics.
     *
     * @param registrationId the registration identifier
     * @param request        the client with updated fields
     */
    @PutMapping("/{registrationId}")
    public void updateClient(@PathVariable String registrationId, @RequestBody OAuth2ClientRequest request) {
        DefaultEulerOAuth2Client model = request.toModel();
        model.setRegistrationId(registrationId);
        this.oauth2ClientService.updateClient(model);
    }

    /**
     * Deletes a client by its registration ID.
     *
     * @param registrationId the registration identifier
     */
    @DeleteMapping("/{registrationId}")
    public void deleteClient(@PathVariable String registrationId) {
        this.oauth2ClientService.deleteClient(registrationId);
    }

    /**
     * Rotates the client secret for a client that authenticates with one.
     *
     * <p>Mints a fresh plaintext secret, persists its hash and returns the
     * plaintext exactly once through {@link OAuth2ClientSecretResponse}.
     * Rotation is rejected for clients whose
     * {@code token_endpoint_auth_method} does not use a shared secret.
     *
     * <p>{@code clientSecretExpiresAt} is left untouched on rotation; any
     * change to the expiry policy belongs to a separate configuration path.
     *
     * @param registrationId the registration identifier
     * @return the one-shot plaintext secret and its current expiry
     * @throws IllegalArgumentException if no client exists for the given ID
     * @throws IllegalStateException    if the client does not use a shared secret
     */
    @PostMapping("/{registrationId}/client-secret")
    public OAuth2ClientSecretResponse rotateClientSecret(@PathVariable String registrationId) {
        EulerOAuth2Client client = this.oauth2ClientService.loadClientByRegistrationId(registrationId);
        if (client == null) {
            throw new IllegalArgumentException("Client not found, registrationId: " + registrationId);
        }
        if (!OAuth2ClientSecretGenerator.requiresClientSecret(client.getTokenEndpointAuthMethod())) {
            throw new IllegalStateException(
                    "Client " + registrationId + " does not use a shared secret (token_endpoint_auth_method="
                            + client.getTokenEndpointAuthMethod() + ")");
        }
        String plaintextSecret = OAuth2ClientSecretGenerator.generate();
        this.oauth2ClientService.updateClientSecret(registrationId, this.passwordEncoder.encode(plaintextSecret));
        return new OAuth2ClientSecretResponse(plaintextSecret, client.getClientSecretExpiresAt());
    }
}
