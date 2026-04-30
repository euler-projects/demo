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

package org.eulerframework.uc.oauth2.entity;

import jakarta.persistence.*;
import org.eulerframework.data.entity.AuditingEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "oauth2_client")
public class OAuth2ClientEntity extends AuditingEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    @Column(name = "client_id_issued_at")
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    @Column(name = "grant_types")
    private String grantTypes;

    @Column(name = "response_types")
    private String responseTypes;

    @Column(name = "redirect_uris")
    private String redirectUris;

    @Column(name = "post_logout_redirect_uris")
    private String postLogoutRedirectUris;

    @Column(name = "scopes")
    private String scopes;

    @Column(name = "jwk_set_url")
    private String jwkSetUrl;

    @Column(name = "jwks")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> jwks;

    @Column(name = "token_endpoint_auth_signing_alg")
    private String tokenEndpointAuthSigningAlgorithm;

    @Column(name = "id_token_signed_response_alg")
    private String idTokenSignedResponseAlgorithm;

    @Column(name = "tls_client_auth_subject_dn")
    private String tlsClientAuthSubjectDN;

    @Column(name = "tls_client_certificate_bound_access_tokens")
    private Boolean tlsClientCertificateBoundAccessTokens;

    @Column(name = "client_settings")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> clientSettings;

    @Column(name = "token_settings")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tokenSettings;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
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

    public String getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String authorizationGrantTypes) {
        this.grantTypes = authorizationGrantTypes;
    }

    public String getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(String responseTypes) {
        this.responseTypes = responseTypes;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(String postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public Map<String, Object> getJwks() {
        return jwks;
    }

    public void setJwks(Map<String, Object> jwks) {
        this.jwks = jwks;
    }

    public String getJwkSetUrl() {
        return jwkSetUrl;
    }

    public void setJwkSetUrl(String jwkSetUrl) {
        this.jwkSetUrl = jwkSetUrl;
    }

    public String getTokenEndpointAuthSigningAlgorithm() {
        return tokenEndpointAuthSigningAlgorithm;
    }

    public void setTokenEndpointAuthSigningAlgorithm(String tokenEndpointAuthenticationSigningAlgorithm) {
        this.tokenEndpointAuthSigningAlgorithm = tokenEndpointAuthenticationSigningAlgorithm;
    }

    public String getTlsClientAuthSubjectDN() {
        return tlsClientAuthSubjectDN;
    }

    public void setTlsClientAuthSubjectDN(String x509CertificateSubjectDN) {
        this.tlsClientAuthSubjectDN = x509CertificateSubjectDN;
    }

    public String getIdTokenSignedResponseAlgorithm() {
        return idTokenSignedResponseAlgorithm;
    }

    public void setIdTokenSignedResponseAlgorithm(String idTokenSignatureAlgorithm) {
        this.idTokenSignedResponseAlgorithm = idTokenSignatureAlgorithm;
    }

    public Boolean getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Boolean x509CertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = x509CertificateBoundAccessTokens;
    }

    public Map<String, Object> getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(Map<String, Object> clientSettings) {
        this.clientSettings = clientSettings;
    }

    public Map<String, Object> getTokenSettings() {
        return tokenSettings;
    }

    public void setTokenSettings(Map<String, Object> tokenSettings) {
        this.tokenSettings = tokenSettings;
    }

    @Override
    public boolean isNew() {
        return this.getCreatedDate() == null;
    }
}
