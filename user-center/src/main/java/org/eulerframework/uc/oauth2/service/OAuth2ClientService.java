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
import org.eulerframework.uc.oauth2.model.OAuth2Client;
import org.eulerframework.uc.oauth2.repository.OAuth2ClientRepository;
import org.eulerframework.uc.oauth2.util.OAuth2ClientModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OAuth2ClientService implements EulerOAuth2ClientService {

    private OAuth2ClientRepository oauth2ClientRepository;

    @Override
    @Transactional
    public EulerOAuth2Client createClient(RegisteredClient registeredClient) {
        EulerOAuth2Client client = new OAuth2Client();
        client.reloadRegisteredClient(registeredClient);
        return this.createClient(client);
    }

    @Override
    @Transactional
    public EulerOAuth2Client createClient(EulerOAuth2Client client) {
        Assert.notNull(client, "client must not be null");
        Assert.isInstanceOf(OAuth2Client.class, client, "client must be an instance of OAuth2Client");

        OAuth2Client model = (OAuth2Client) client;
        OAuth2ClientEntity entity = OAuth2ClientModelUtils.toOAuth2ClientEntity(model);
        entity.setId(null); // ensure ID is generated
        OAuth2ClientEntity saved = this.oauth2ClientRepository.save(entity);
        return OAuth2ClientModelUtils.toOAuth2Client(saved);
    }

    @Override
    public EulerOAuth2Client loadClientByClientId(String clientId) {
        return this.oauth2ClientRepository.findByClientId(clientId)
                .map(OAuth2ClientModelUtils::toOAuth2Client)
                .orElse(null);
    }

    @Override
    public EulerOAuth2Client loadClientByRegistrationId(String registrationId) {
        return this.oauth2ClientRepository.findById(registrationId)
                .map(OAuth2ClientModelUtils::toOAuth2Client)
                .orElse(null);
    }

    @Override
    public List<EulerOAuth2Client> listClients(int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        return this.oauth2ClientRepository.findAll(PageRequest.of(page, limit))
                .stream()
                .map(OAuth2ClientModelUtils::toOAuth2Client)
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

        // Basic properties (patch semantics: only update when non-null)
        Optional.ofNullable(client.getClientId()).ifPresent(entity::setClientId);
        Optional.ofNullable(client.getClientIdIssuedAt()).ifPresent(entity::setClientIdIssuedAt);
        Optional.ofNullable(client.getClientSecret()).ifPresent(entity::setClientSecret);
        Optional.ofNullable(client.getClientSecretExpiresAt()).ifPresent(entity::setClientSecretExpiresAt);
        Optional.ofNullable(client.getClientName()).ifPresent(entity::setClientName);

        // RFC 7591 fields
        Optional.ofNullable(client.getTokenEndpointAuthMethod()).ifPresent(entity::setTokenEndpointAuthMethod);
        Optional.ofNullable(client.getGrantTypes()).ifPresent(types ->
                entity.setAuthorizationGrantTypes(String.join(",", types)));
        Optional.ofNullable(client.getResponseTypes()).ifPresent(types ->
                entity.setResponseTypes(String.join(",", types)));
        Optional.ofNullable(client.getRedirectUris()).ifPresent(uris ->
                entity.setRedirectUris(String.join(",", uris)));
        Optional.ofNullable(client.getPostLogoutRedirectUris()).ifPresent(uris ->
                entity.setPostLogoutRedirectUris(String.join(",", uris)));
        Optional.ofNullable(client.getScopes()).ifPresent(scopes ->
                entity.setScopes(String.join(",", scopes)));
        Optional.ofNullable(client.getJwksUri()).ifPresent(entity::setJwkSetUrl);
        Optional.ofNullable(client.getJwks()).ifPresent(jwks ->
                entity.setJwks(JacksonUtils.writeValueAsString(jwks)));

        // Promoted standard fields (OIDC Dynamic Registration / RFC 8705)
        Optional.ofNullable(client.getTokenEndpointAuthSigningAlgorithm())
                .ifPresent(entity::setTokenEndpointAuthenticationSigningAlgorithm);
        Optional.ofNullable(client.getTlsClientAuthSubjectDN())
                .ifPresent(entity::setX509CertificateSubjectDN);
        Optional.ofNullable(client.getIdTokenSignedResponseAlgorithm())
                .ifPresent(entity::setIdTokenSignatureAlgorithm);
        Optional.ofNullable(client.getTlsClientCertificateBoundAccessTokens())
                .ifPresent(entity::setX509CertificateBoundAccessTokens);

        // ClientSettings patch
        EulerOAuth2ClientSettings cs = client.getClientSettings();
        if (cs != null) {
            Optional.ofNullable(cs.getRequireProofKey()).ifPresent(entity::setRequireProofKey);
            Optional.ofNullable(cs.getRequireAuthorizationConsent()).ifPresent(entity::setRequireAuthorizationConsent);
        }

        // TokenSettings patch
        EulerOAuth2TokenSettings ts = client.getTokenSettings();
        if (ts != null) {
            Optional.ofNullable(ts.getAuthorizationCodeTimeToLive()).ifPresent(entity::setAuthorizationCodeTimeToLive);
            Optional.ofNullable(ts.getAccessTokenTimeToLive()).ifPresent(entity::setAccessTokenTimeToLive);
            Optional.ofNullable(ts.getAccessTokenFormat()).ifPresent(entity::setAccessTokenFormat);
            Optional.ofNullable(ts.getDeviceCodeTimeToLive()).ifPresent(entity::setDeviceCodeTimeToLive);
            Optional.ofNullable(ts.getReuseRefreshTokens()).ifPresent(entity::setReuseRefreshTokens);
            Optional.ofNullable(ts.getRefreshTokenTimeToLive()).ifPresent(entity::setRefreshTokenTimeToLive);
        }

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
