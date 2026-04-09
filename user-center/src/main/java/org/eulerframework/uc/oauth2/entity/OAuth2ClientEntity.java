package org.eulerframework.uc.oauth2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingUUIDEntity;

import java.time.Instant;

@Entity
@Table(name = "t_oauth2_client")
public class OAuth2ClientEntity extends AuditingUUIDEntity {

    // --- Basic properties ---

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

    // --- RFC 7591 properties ---

    @Column(name = "token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    @Column(name = "authorization_grant_types", nullable = false)
    private String authorizationGrantTypes;

    @Column(name = "response_types")
    private String responseTypes;

    @Column(name = "redirect_uris")
    private String redirectUris;

    @Column(name = "post_logout_redirect_uris")
    private String postLogoutRedirectUris;

    @Column(name = "scopes")
    private String scopes;

    // --- ClientSettings (flat) ---

    @Column(name = "jwks", columnDefinition = "TEXT")
    private String jwks;

    @Column(name = "require_proof_key")
    private Boolean requireProofKey;

    @Column(name = "require_authorization_consent")
    private Boolean requireAuthorizationConsent;

    @Column(name = "jwk_set_url")
    private String jwkSetUrl;

    @Column(name = "token_endpoint_authentication_signing_algorithm")
    private String tokenEndpointAuthenticationSigningAlgorithm;

    @Column(name = "x509_certificate_subject_dn")
    private String x509CertificateSubjectDN;

    // --- TokenSettings (flat, durations stored as seconds) ---

    @Column(name = "authorization_code_time_to_live")
    private Long authorizationCodeTimeToLive;

    @Column(name = "access_token_time_to_live")
    private Long accessTokenTimeToLive;

    @Column(name = "access_token_format")
    private String accessTokenFormat;

    @Column(name = "device_code_time_to_live")
    private Long deviceCodeTimeToLive;

    @Column(name = "reuse_refresh_tokens")
    private Boolean reuseRefreshTokens;

    @Column(name = "refresh_token_time_to_live")
    private Long refreshTokenTimeToLive;

    @Column(name = "id_token_signature_algorithm")
    private String idTokenSignatureAlgorithm;

    @Column(name = "x509_certificate_bound_access_tokens")
    private Boolean x509CertificateBoundAccessTokens;

    // --- Getters and Setters ---

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

    public String getAuthorizationGrantTypes() {
        return authorizationGrantTypes;
    }

    public void setAuthorizationGrantTypes(String authorizationGrantTypes) {
        this.authorizationGrantTypes = authorizationGrantTypes;
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

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public Boolean getRequireProofKey() {
        return requireProofKey;
    }

    public void setRequireProofKey(Boolean requireProofKey) {
        this.requireProofKey = requireProofKey;
    }

    public Boolean getRequireAuthorizationConsent() {
        return requireAuthorizationConsent;
    }

    public void setRequireAuthorizationConsent(Boolean requireAuthorizationConsent) {
        this.requireAuthorizationConsent = requireAuthorizationConsent;
    }

    public String getJwkSetUrl() {
        return jwkSetUrl;
    }

    public void setJwkSetUrl(String jwkSetUrl) {
        this.jwkSetUrl = jwkSetUrl;
    }

    public String getTokenEndpointAuthenticationSigningAlgorithm() {
        return tokenEndpointAuthenticationSigningAlgorithm;
    }

    public void setTokenEndpointAuthenticationSigningAlgorithm(String tokenEndpointAuthenticationSigningAlgorithm) {
        this.tokenEndpointAuthenticationSigningAlgorithm = tokenEndpointAuthenticationSigningAlgorithm;
    }

    public String getX509CertificateSubjectDN() {
        return x509CertificateSubjectDN;
    }

    public void setX509CertificateSubjectDN(String x509CertificateSubjectDN) {
        this.x509CertificateSubjectDN = x509CertificateSubjectDN;
    }

    public Long getAuthorizationCodeTimeToLive() {
        return authorizationCodeTimeToLive;
    }

    public void setAuthorizationCodeTimeToLive(Long authorizationCodeTimeToLive) {
        this.authorizationCodeTimeToLive = authorizationCodeTimeToLive;
    }

    public Long getAccessTokenTimeToLive() {
        return accessTokenTimeToLive;
    }

    public void setAccessTokenTimeToLive(Long accessTokenTimeToLive) {
        this.accessTokenTimeToLive = accessTokenTimeToLive;
    }

    public String getAccessTokenFormat() {
        return accessTokenFormat;
    }

    public void setAccessTokenFormat(String accessTokenFormat) {
        this.accessTokenFormat = accessTokenFormat;
    }

    public Long getDeviceCodeTimeToLive() {
        return deviceCodeTimeToLive;
    }

    public void setDeviceCodeTimeToLive(Long deviceCodeTimeToLive) {
        this.deviceCodeTimeToLive = deviceCodeTimeToLive;
    }

    public Boolean getReuseRefreshTokens() {
        return reuseRefreshTokens;
    }

    public void setReuseRefreshTokens(Boolean reuseRefreshTokens) {
        this.reuseRefreshTokens = reuseRefreshTokens;
    }

    public Long getRefreshTokenTimeToLive() {
        return refreshTokenTimeToLive;
    }

    public void setRefreshTokenTimeToLive(Long refreshTokenTimeToLive) {
        this.refreshTokenTimeToLive = refreshTokenTimeToLive;
    }

    public String getIdTokenSignatureAlgorithm() {
        return idTokenSignatureAlgorithm;
    }

    public void setIdTokenSignatureAlgorithm(String idTokenSignatureAlgorithm) {
        this.idTokenSignatureAlgorithm = idTokenSignatureAlgorithm;
    }

    public Boolean getX509CertificateBoundAccessTokens() {
        return x509CertificateBoundAccessTokens;
    }

    public void setX509CertificateBoundAccessTokens(Boolean x509CertificateBoundAccessTokens) {
        this.x509CertificateBoundAccessTokens = x509CertificateBoundAccessTokens;
    }
}
