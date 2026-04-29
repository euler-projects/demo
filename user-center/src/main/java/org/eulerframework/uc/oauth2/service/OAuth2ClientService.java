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

package org.eulerframework.uc.oauth2.service;

import org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2ClientService;
import org.eulerframework.uc.oauth2.entity.OAuth2ClientEntity;
import org.eulerframework.uc.oauth2.repository.OAuth2ClientRepository;
import org.eulerframework.uc.oauth2.util.OAuth2ClientModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OAuth2ClientService implements EulerOAuth2ClientService {

    private OAuth2ClientRepository oauth2ClientRepository;

    @Override
    @Transactional
    public EulerOAuth2Client createClient(EulerOAuth2Client client) {
        Assert.notNull(client, "client must not be null");
        Assert.isInstanceOf(DefaultEulerOAuth2Client.class, client, "client must be an instance of OAuth2Client");

        DefaultEulerOAuth2Client model = (DefaultEulerOAuth2Client) client;
        OAuth2ClientEntity entity = OAuth2ClientModelUtils.toOAuth2ClientEntity(model);

        // registrationId / clientId / clientIdIssuedAt are fully owned by the server
        // on this path; any value carried by the caller is deliberately overwritten.
        entity.setId(UUID.randomUUID().toString());
        entity.setClientId(generateClientId());
        entity.setClientIdIssuedAt(Instant.now());

        OAuth2ClientEntity saved = this.oauth2ClientRepository.save(entity);
        return OAuth2ClientModelUtils.toEulerOAuth2Client(saved);
    }

    @Override
    @Transactional
    public EulerOAuth2Client createClient(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient must not be null");
        Assert.hasText(registeredClient.getId(), "registrationId must not be empty");
        Assert.hasText(registeredClient.getClientId(), "clientId must not be empty");

        DefaultEulerOAuth2Client model = new DefaultEulerOAuth2Client();
        model.reloadRegisteredClient(registeredClient);

        OAuth2ClientEntity entity = OAuth2ClientModelUtils.toOAuth2ClientEntity(model);

        // RegisteredClient carries a fully-assembled registration; the server
        // only back-fills clientIdIssuedAt when the caller left it unset.
        if (entity.getClientIdIssuedAt() == null) {
            entity.setClientIdIssuedAt(Instant.now());
        }

        OAuth2ClientEntity saved = this.oauth2ClientRepository.save(entity);
        return OAuth2ClientModelUtils.toEulerOAuth2Client(saved);
    }

    @Override
    public EulerOAuth2Client loadClientByClientId(String clientId) {
        return this.oauth2ClientRepository.findByClientId(clientId)
                .map(OAuth2ClientModelUtils::toEulerOAuth2Client)
                .orElse(null);
    }

    @Override
    public EulerOAuth2Client loadClientByRegistrationId(String registrationId) {
        return this.oauth2ClientRepository.findById(registrationId)
                .map(OAuth2ClientModelUtils::toEulerOAuth2Client)
                .orElse(null);
    }

    @Override
    public List<EulerOAuth2Client> listClients(int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        return this.oauth2ClientRepository.findAll(PageRequest.of(page, limit))
                .stream()
                .map(OAuth2ClientModelUtils::toEulerOAuth2Client)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateClient(EulerOAuth2Client client) {
        Assert.notNull(client, "client must not be null");
        Assert.notNull(client.getRegistrationId(), "registrationId must not be null");

        OAuth2ClientEntity entity = this.oauth2ClientRepository.findById(client.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client not found, registrationId: " + client.getRegistrationId()));

        OAuth2ClientModelUtils.updateOAuth2ClientEntity(client, entity);

        this.oauth2ClientRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateClient(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient must not be null");
        Assert.notNull(registeredClient.getId(), "registrationId must not be null");

        OAuth2ClientEntity entity = this.oauth2ClientRepository.findById(registeredClient.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client not found, registrationId: " + registeredClient.getId()));

        DefaultEulerOAuth2Client model = new DefaultEulerOAuth2Client();
        model.reloadRegisteredClient(registeredClient);

        OAuth2ClientModelUtils.replaceOAuth2ClientEntity(model, entity);

        this.oauth2ClientRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteClient(String registrationId) {
        this.oauth2ClientRepository.deleteById(registrationId);
    }

    @Override
    @Transactional
    public void updateClientSecret(String registrationId, String clientSecret) {
        OAuth2ClientEntity entity = this.oauth2ClientRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client not found, registrationId: " + registrationId));
        entity.setClientSecret(clientSecret);
        this.oauth2ClientRepository.save(entity);
    }

    @Autowired
    public void setOauth2ClientRepository(OAuth2ClientRepository oauth2ClientRepository) {
        this.oauth2ClientRepository = oauth2ClientRepository;
    }

    /**
     * 256 bits of entropy behind the generated {@code clientId}, matching the
     * strength of a minted client secret.
     */
    private static final int CLIENT_ID_RANDOM_BYTES = 32;

    private static final SecureRandom CLIENT_ID_RANDOM = new SecureRandom();
    private static final Base64.Encoder CLIENT_ID_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * Mints a fresh {@code clientId} as the URL-safe, unpadded Base64 encoding
     * of 256 random bits ({@value #CLIENT_ID_RANDOM_BYTES} bytes). The result
     * is 43 characters drawn from {@code [A-Za-z0-9_-]}, usable verbatim in
     * HTTP headers, form bodies and URL path segments without further escaping.
     *
     * @return the freshly generated {@code clientId}
     */
    private static String generateClientId() {
        byte[] random = new byte[CLIENT_ID_RANDOM_BYTES];
        CLIENT_ID_RANDOM.nextBytes(random);
        return CLIENT_ID_ENCODER.encodeToString(random);
    }
}
