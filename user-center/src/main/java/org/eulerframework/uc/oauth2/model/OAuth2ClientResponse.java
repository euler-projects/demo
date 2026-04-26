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

import com.nimbusds.jose.jwk.JWKSet;
import org.eulerframework.security.jackson.JWKSetDeserializer;
import org.eulerframework.security.jackson.JWKSetSerializer;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Outbound payload for the Admin OAuth 2.0 Client API.
 *
 * <p>Compared to {@link OAuth2ClientRequest}, this DTO additionally surfaces
 * the read-only, server-owned attributes defined by
 * <a href="https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.1">RFC&nbsp;7591 §3.2.1</a>:
 * {@code registrationId}, {@code clientId} and {@code clientIdIssuedAt}.
 *
 * <p>{@code clientSecret} is intentionally <em>not</em> exposed here. The
 * credential persisted by the service is an opaque encoded form (e.g. a
 * password-encoder hash), which is useless to operators and would merely
 * leak the on-disk representation if returned. A freshly minted plaintext
 * secret is instead carried by the dedicated one-shot
 * {@link OAuth2ClientCreatedResponse} (on creation) and
 * {@link OAuth2ClientSecretResponse} (on rotation).
 */
public class OAuth2ClientResponse {

    private String registrationId;
    private String clientId;
    private Instant clientIdIssuedAt;
    private Instant clientSecretExpiresAt;
    private String clientName;
    private String tokenEndpointAuthMethod;
    private Set<String> grantTypes;
    private Set<String> responseTypes;
    private Set<String> redirectUris;
    private Set<String> postLogoutRedirectUris;
    private Set<String> scopes;
    private String jwksUri;
    @JsonSerialize(using = JWKSetSerializer.class)
    @JsonDeserialize(using = JWKSetDeserializer.class)
    private JWKSet jwks;
    private String tokenEndpointAuthSigningAlgorithm;
    private String idTokenSignedResponseAlgorithm;
    private String tlsClientAuthSubjectDN;
    private Boolean tlsClientCertificateBoundAccessTokens;
    private OAuth2ClientSettingsDto clientSettings;
    private OAuth2TokenSettingsDto tokenSettings;

    // ==================== getters / setters ====================

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Instant getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(Instant clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public Instant getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(Instant clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public Set<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(Set<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public Set<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(Set<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Set<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(Set<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public JWKSet getJwks() {
        return jwks;
    }

    public void setJwks(JWKSet jwks) {
        this.jwks = jwks;
    }

    public String getTokenEndpointAuthSigningAlgorithm() {
        return tokenEndpointAuthSigningAlgorithm;
    }

    public void setTokenEndpointAuthSigningAlgorithm(String tokenEndpointAuthSigningAlgorithm) {
        this.tokenEndpointAuthSigningAlgorithm = tokenEndpointAuthSigningAlgorithm;
    }

    public String getIdTokenSignedResponseAlgorithm() {
        return idTokenSignedResponseAlgorithm;
    }

    public void setIdTokenSignedResponseAlgorithm(String idTokenSignedResponseAlgorithm) {
        this.idTokenSignedResponseAlgorithm = idTokenSignedResponseAlgorithm;
    }

    public String getTlsClientAuthSubjectDN() {
        return tlsClientAuthSubjectDN;
    }

    public void setTlsClientAuthSubjectDN(String tlsClientAuthSubjectDN) {
        this.tlsClientAuthSubjectDN = tlsClientAuthSubjectDN;
    }

    public Boolean getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Boolean tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }

    public OAuth2ClientSettingsDto getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(OAuth2ClientSettingsDto clientSettings) {
        this.clientSettings = clientSettings;
    }

    public OAuth2TokenSettingsDto getTokenSettings() {
        return tokenSettings;
    }

    public void setTokenSettings(OAuth2TokenSettingsDto tokenSettings) {
        this.tokenSettings = tokenSettings;
    }

    // ==================== Conversion ====================

    /**
     * Projects an {@link EulerOAuth2Client} into its transport form.
     *
     * <p>The {@code clientSecret} attribute of the domain model is deliberately
     * dropped during the projection: what the service layer hands back is the
     * encoded form stored on disk, which must never be surfaced on the wire.
     *
     * @param model the domain model, or {@code null}
     * @return the DTO, or {@code null} when the source is {@code null}
     */
    public static OAuth2ClientResponse fromModel(EulerOAuth2Client model) {
        if (model == null) {
            return null;
        }
        OAuth2ClientResponse dto = new OAuth2ClientResponse();
        populateResponse(dto, model);
        return dto;
    }

    /**
     * Copies every {@link EulerOAuth2Client} attribute that is safe to expose
     * on the wire into the supplied response DTO. Credential-bearing fields
     * ({@code clientSecret}) are intentionally excluded.
     *
     * <p>Shared with {@link OAuth2ClientCreatedResponse} so that the
     * registration endpoint can return the full client state alongside the
     * one-shot plaintext secret without duplicating the projection logic.
     *
     * @param dto   the target DTO to populate (non-{@code null})
     * @param model the source domain model (non-{@code null})
     */
    static void populateResponse(OAuth2ClientResponse dto, EulerOAuth2Client model) {
        dto.registrationId = model.getRegistrationId();
        dto.clientId = model.getClientId();
        dto.clientIdIssuedAt = model.getClientIdIssuedAt();
        dto.clientSecretExpiresAt = model.getClientSecretExpiresAt();
        dto.clientName = model.getClientName();
        dto.tokenEndpointAuthMethod = model.getTokenEndpointAuthMethod();
        dto.grantTypes = model.getGrantTypes();
        dto.responseTypes = toSet(model.getResponseTypes());
        dto.redirectUris = toSet(model.getRedirectUris());
        dto.postLogoutRedirectUris = toSet(model.getPostLogoutRedirectUris());
        dto.scopes = toSet(model.getScopes());
        dto.jwksUri = model.getJwksUri();
        dto.jwks = model.getJwks();
        dto.tokenEndpointAuthSigningAlgorithm = model.getTokenEndpointAuthSigningAlgorithm();
        dto.idTokenSignedResponseAlgorithm = model.getIdTokenSignedResponseAlgorithm();
        dto.tlsClientAuthSubjectDN = model.getTlsClientAuthSubjectDN();
        dto.tlsClientCertificateBoundAccessTokens = model.getTlsClientCertificateBoundAccessTokens();
        dto.clientSettings = OAuth2ClientSettingsDto.fromModel(model.getClientSettings());
        dto.tokenSettings = OAuth2TokenSettingsDto.fromModel(model.getTokenSettings());
    }

    private static Set<String> toSet(Collection<String> collection) {
        return collection == null ? null : new LinkedHashSet<>(collection);
    }
}
