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

import org.eulerframework.common.util.jackson.JacksonUtils;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2ClientService;
import org.eulerframework.security.oauth2.server.authorization.settings.EulerOAuth2ClientSettings;
import org.eulerframework.security.oauth2.server.authorization.settings.EulerOAuth2TokenSettings;
import org.eulerframework.uc.oauth2.entity.OAuth2ClientEntity;
import org.eulerframework.uc.oauth2.repository.OAuth2ClientRepository;
import org.eulerframework.uc.oauth2.util.OAuth2ClientModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OAuth2ClientService implements EulerOAuth2ClientService {

    private OAuth2ClientRepository oauth2ClientRepository;

    @Override
    @Transactional
    public EulerOAuth2Client createClient(RegisteredClient registeredClient) {
        EulerOAuth2Client client = new org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client();
        client.reloadRegisteredClient(registeredClient);
        return this.createClient(client);
    }

    @Override
    @Transactional
    public EulerOAuth2Client createClient(EulerOAuth2Client client) {
        Assert.notNull(client, "client must not be null");
        Assert.isInstanceOf(org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client.class, client, "client must be an instance of OAuth2Client");

        org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client model = (org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client) client;
        OAuth2ClientEntity entity = OAuth2ClientModelUtils.toOAuth2ClientEntity(model);
        entity.setId(null); // ensure ID is generated
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
}
