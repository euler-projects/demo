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
import org.eulerframework.model.AbstractAuditingModel;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.settings.EulerConfigurationSettingNames;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class OAuth2Client extends AbstractAuditingModel implements EulerOAuth2Client {

    private String registrationId;
    private String clientId;
    private Instant clientIdIssuedAt;
    private String clientSecret;
    private Instant clientSecretExpiresAt;
    private String clientName;
    private Set<String> redirectUris;
    private String tokenEndpointAuthMethod;
    private Set<String> grantTypes;
    private Set<String> responseTypes;
    private Set<String> scopes;
    private String jwksUri;
    private Map<String, Object> jwks;
    private Set<String> postLogoutRedirectUris;
    private String tokenEndpointAuthSigningAlgorithm;
    private String idTokenSignedResponseAlgorithm;
    private String tlsClientAuthSubjectDN;
    private Boolean tlsClientCertificateBoundAccessTokens;

    private OAuth2ClientSettings clientSettings = new OAuth2ClientSettings();
    private OAuth2TokenSettings tokenSettings = new OAuth2TokenSettings();

    // ==================== EulerOAuth2Client getters/setters ====================

    @Override
    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Instant getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(Instant clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Override
    public Instant getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(Instant clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    @Override
    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    @Override
    public Set<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(Set<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    @Override
    public Set<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(Set<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    @Override
    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @Override
    public Map<String, Object> getJwks() {
        return jwks;
    }

    public void setJwks(Map<String, Object> jwks) {
        this.jwks = jwks;
    }

    @Override
    public Set<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(Set<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    @Override
    public String getTokenEndpointAuthSigningAlgorithm() {
        return tokenEndpointAuthSigningAlgorithm;
    }

    public void setTokenEndpointAuthSigningAlgorithm(String tokenEndpointAuthSigningAlgorithm) {
        this.tokenEndpointAuthSigningAlgorithm = tokenEndpointAuthSigningAlgorithm;
    }

    @Override
    public String getIdTokenSignedResponseAlgorithm() {
        return idTokenSignedResponseAlgorithm;
    }

    public void setIdTokenSignedResponseAlgorithm(String idTokenSignedResponseAlgorithm) {
        this.idTokenSignedResponseAlgorithm = idTokenSignedResponseAlgorithm;
    }

    @Override
    public String getTlsClientAuthSubjectDN() {
        return tlsClientAuthSubjectDN;
    }

    public void setTlsClientAuthSubjectDN(String tlsClientAuthSubjectDN) {
        this.tlsClientAuthSubjectDN = tlsClientAuthSubjectDN;
    }

    @Override
    public Boolean getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Boolean tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }

    @Override
    public OAuth2ClientSettings getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(OAuth2ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }

    @Override
    public OAuth2TokenSettings getTokenSettings() {
        return tokenSettings;
    }

    public void setTokenSettings(OAuth2TokenSettings tokenSettings) {
        this.tokenSettings = tokenSettings;
    }

    // ==================== CredentialsContainer ====================

    @Override
    public void eraseCredentials() {
        this.clientSecret = null;
    }

    // ==================== Bridge ====================

    @Override
    @SuppressWarnings("unchecked")
    public void reloadRegisteredClient(RegisteredClient registeredClient) {
        this.registrationId = registeredClient.getId();
        this.clientId = registeredClient.getClientId();
        this.clientIdIssuedAt = registeredClient.getClientIdIssuedAt();
        this.clientSecret = registeredClient.getClientSecret();
        this.clientSecretExpiresAt = registeredClient.getClientSecretExpiresAt();
        this.clientName = registeredClient.getClientName();

        // token_endpoint_auth_method: RFC 7591 requires exactly one method
        if (registeredClient.getClientAuthenticationMethods() != null
                && registeredClient.getClientAuthenticationMethods().size() > 1) {
            throw new IllegalStateException(
                    "RFC 7591 requires exactly one token_endpoint_auth_method, but found "
                            + registeredClient.getClientAuthenticationMethods().size());
        }
        this.tokenEndpointAuthMethod = Optional.ofNullable(registeredClient.getClientAuthenticationMethods())
                .flatMap(methods -> methods.stream().findFirst())
                .map(ClientAuthenticationMethod::getValue)
                .orElse(null);

        // grant_types: convert to Set<String>
        this.grantTypes = Optional.ofNullable(registeredClient.getAuthorizationGrantTypes())
                .map(types -> types.stream().map(AuthorizationGrantType::getValue).collect(Collectors.toSet()))
                .orElse(null);

        // response_types: derive from grant types (authorization_code -> code)
        if (this.grantTypes != null && this.grantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())) {
            this.responseTypes = Set.of("code");
        } else {
            this.responseTypes = null;
        }

        this.redirectUris = Optional.ofNullable(registeredClient.getRedirectUris())
                .map(HashSet::new)
                .orElse(null);
        this.postLogoutRedirectUris = Optional.ofNullable(registeredClient.getPostLogoutRedirectUris())
                .map(HashSet::new)
                .orElse(null);
        this.scopes = Optional.ofNullable(registeredClient.getScopes())
                .map(HashSet::new)
                .orElse(null);

        // ClientSettings -> flat fields
        ClientSettings cs = registeredClient.getClientSettings();
        if (cs != null) {
            this.jwksUri = cs.getJwkSetUrl();

            // jwks: read from extended setting, convert JWKSet/Map to Map<String, Object>
            Object jwksValue = cs.getSettings().get(EulerConfigurationSettingNames.Client.JWKS);
            if (jwksValue == null) {
                this.jwks = null;
            } else if (jwksValue instanceof JWKSet jwkSet) {
                this.jwks = jwkSet.toJSONObject();
            } else if (jwksValue instanceof Map) {
                this.jwks = (Map<String, Object>) jwksValue;
            } else {
                throw new IllegalStateException(
                        "Unsupported jwks type: " + jwksValue.getClass().getName());
            }

            if (this.clientSettings == null) {
                this.clientSettings = new OAuth2ClientSettings();
            }
            this.clientSettings.setRequireProofKey(cs.isRequireProofKey());
            this.clientSettings.setRequireAuthorizationConsent(cs.isRequireAuthorizationConsent());

            // Promoted to top-level (OIDC Dynamic Registration / RFC 8705)
            JwsAlgorithm sigAlg = cs.getTokenEndpointAuthenticationSigningAlgorithm();
            this.tokenEndpointAuthSigningAlgorithm = sigAlg != null ? sigAlg.getName() : null;

            this.tlsClientAuthSubjectDN = cs.getX509CertificateSubjectDN();
        } else {
            this.jwksUri = null;
            this.jwks = null;
            this.tokenEndpointAuthSigningAlgorithm = null;
            this.tlsClientAuthSubjectDN = null;
            this.clientSettings = null;
        }

        // TokenSettings -> flat fields
        TokenSettings ts = registeredClient.getTokenSettings();
        if (ts != null) {
            if (this.tokenSettings == null) {
                this.tokenSettings = new OAuth2TokenSettings();
            }
            this.tokenSettings.setAuthorizationCodeTimeToLive(Optional.ofNullable(ts.getAuthorizationCodeTimeToLive())
                    .map(Duration::getSeconds).orElse(null));
            this.tokenSettings.setAccessTokenTimeToLive(Optional.ofNullable(ts.getAccessTokenTimeToLive())
                    .map(Duration::getSeconds).orElse(null));
            this.tokenSettings.setAccessTokenFormat(Optional.ofNullable(ts.getAccessTokenFormat())
                    .map(OAuth2TokenFormat::getValue).orElse(null));
            this.tokenSettings.setDeviceCodeTimeToLive(Optional.ofNullable(ts.getDeviceCodeTimeToLive())
                    .map(Duration::getSeconds).orElse(null));
            this.tokenSettings.setReuseRefreshTokens(ts.isReuseRefreshTokens());
            this.tokenSettings.setRefreshTokenTimeToLive(Optional.ofNullable(ts.getRefreshTokenTimeToLive())
                    .map(Duration::getSeconds).orElse(null));

            // Promoted to top-level (OIDC Dynamic Registration / RFC 8705)
            SignatureAlgorithm idTokenSigAlg = ts.getIdTokenSignatureAlgorithm();
            this.idTokenSignedResponseAlgorithm = idTokenSigAlg != null ? idTokenSigAlg.getName() : null;

            this.tlsClientCertificateBoundAccessTokens = ts.isX509CertificateBoundAccessTokens();
        } else {
            this.idTokenSignedResponseAlgorithm = null;
            this.tlsClientCertificateBoundAccessTokens = null;
            this.tokenSettings = null;
        }
    }
}
