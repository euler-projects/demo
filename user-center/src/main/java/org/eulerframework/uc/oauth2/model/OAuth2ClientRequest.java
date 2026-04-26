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
import org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Set;

/**
 * Inbound payload for the Admin OAuth 2.0 Client API.
 *
 * <p>The field set follows RFC&nbsp;7591 client metadata, with OIDC Dynamic
 * Client Registration and RFC&nbsp;8705 extensions layered on top, plus the
 * Euler-specific client/token settings blocks. The following RFC-governed
 * attributes are deliberately omitted from this DTO because they are owned
 * exclusively by the server:
 * <ul>
 *   <li>{@code registrationId} &mdash; for {@code PUT} it is supplied via the
 *       URL path variable; for {@code POST} it is assigned by the server.</li>
 *   <li>{@code clientId} &mdash; minted by the server per
 *       <a href="https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.1">
 *       RFC&nbsp;7591 §3.2.1</a>. Accepting a caller-supplied value would
 *       compromise uniqueness and collision guarantees.</li>
 *   <li>{@code clientIdIssuedAt} &mdash; a server-minted timestamp per
 *       RFC&nbsp;7591 §3.2.1.</li>
 *   <li>{@code clientSecret} &mdash; generated server-side as a
 *       cryptographically strong random value according to a mandatory
 *       policy; clients must not propose their own secret.</li>
 *   <li>{@code clientSecretExpiresAt} &mdash; derived by the server from
 *       the rotation policy associated with the generated secret.</li>
 * </ul>
 */
public class OAuth2ClientRequest {

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
     * Materializes a {@link DefaultEulerOAuth2Client} from this request.
     *
     * <p>Absent settings blocks are propagated as {@code null} so that the
     * service layer's patch-style update can distinguish "unchanged" from
     * "reset to defaults". The server-owned identifiers and credentials
     * ({@code registrationId}, {@code clientId}, {@code clientSecret},
     * {@code clientSecretExpiresAt}) are all left {@code null} here and are
     * expected to be populated downstream &mdash; either by the controller
     * (from the URL path) or by the service (when issuing new identifiers
     * and generating the random secret per policy).
     *
     * @return the populated domain model
     */
    public DefaultEulerOAuth2Client toModel() {
        DefaultEulerOAuth2Client model = new DefaultEulerOAuth2Client();
        model.setClientName(this.clientName);
        model.setTokenEndpointAuthMethod(this.tokenEndpointAuthMethod);
        model.setGrantTypes(this.grantTypes);
        model.setResponseTypes(this.responseTypes);
        model.setRedirectUris(this.redirectUris);
        model.setPostLogoutRedirectUris(this.postLogoutRedirectUris);
        model.setScopes(this.scopes);
        model.setJwksUri(this.jwksUri);
        model.setJwks(this.jwks);
        model.setTokenEndpointAuthSigningAlgorithm(this.tokenEndpointAuthSigningAlgorithm);
        model.setIdTokenSignedResponseAlgorithm(this.idTokenSignedResponseAlgorithm);
        model.setTlsClientAuthSubjectDN(this.tlsClientAuthSubjectDN);
        model.setTlsClientCertificateBoundAccessTokens(this.tlsClientCertificateBoundAccessTokens);
        if (this.clientSettings != null) {
            model.setClientSettings(this.clientSettings.toModel());
        }
        if (this.tokenSettings != null) {
            model.setTokenSettings(this.tokenSettings.toModel());
        }
        return model;
    }
}
