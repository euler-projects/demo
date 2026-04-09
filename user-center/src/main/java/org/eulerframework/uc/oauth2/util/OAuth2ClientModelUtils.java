package org.eulerframework.uc.oauth2.util;

import org.eulerframework.common.util.jackson.JacksonUtils;
import org.eulerframework.data.util.AuditingEntityUtils;
import org.eulerframework.uc.oauth2.entity.OAuth2ClientEntity;
import org.eulerframework.uc.oauth2.model.OAuth2Client;
import org.eulerframework.uc.oauth2.model.OAuth2ClientSettings;
import org.eulerframework.uc.oauth2.model.OAuth2TokenSettings;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class OAuth2ClientModelUtils {

    /**
     * Converts an {@link OAuth2ClientEntity} to an {@link OAuth2Client} model.
     *
     * @param entity the entity to convert
     * @return the model, or {@code null} if the entity is {@code null}
     */
    public static OAuth2Client toOAuth2Client(OAuth2ClientEntity entity) {
        if (entity == null) {
            return null;
        }

        OAuth2Client model = new OAuth2Client();

        model.setRegistrationId(entity.getId());
        model.setClientId(entity.getClientId());
        model.setClientIdIssuedAt(entity.getClientIdIssuedAt());
        model.setClientSecret(entity.getClientSecret());
        model.setClientSecretExpiresAt(entity.getClientSecretExpiresAt());
        model.setClientName(entity.getClientName());

        // token_endpoint_auth_method: direct mapping
        model.setTokenEndpointAuthMethod(entity.getTokenEndpointAuthMethod());

        // grant_types: comma-separated -> Set<String>
        model.setGrantTypes(commaDelimitedToSet(entity.getAuthorizationGrantTypes()));

        // response_types: comma-separated -> Set<String>
        model.setResponseTypes(commaDelimitedToSet(entity.getResponseTypes()));

        model.setRedirectUris(commaDelimitedToSet(entity.getRedirectUris()));
        model.setPostLogoutRedirectUris(commaDelimitedToSet(entity.getPostLogoutRedirectUris()));
        model.setScopes(commaDelimitedToSet(entity.getScopes()));

        // ClientSettings fields
        OAuth2ClientSettings cs = new OAuth2ClientSettings();
        cs.setRequireProofKey(entity.getRequireProofKey());
        cs.setRequireAuthorizationConsent(entity.getRequireAuthorizationConsent());
        model.setClientSettings(cs);

        model.setJwksUri(entity.getJwkSetUrl());

        // Promoted top-level fields (OIDC Dynamic Registration / RFC 8705)
        model.setTokenEndpointAuthSigningAlgorithm(entity.getTokenEndpointAuthenticationSigningAlgorithm());
        model.setTlsClientAuthSubjectDN(entity.getX509CertificateSubjectDN());

        // jwks: JSON string -> Map<String, Object>
        String jwksJson = entity.getJwks();
        if (StringUtils.hasText(jwksJson)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> jwksMap = JacksonUtils.readValue(jwksJson, Map.class);
            model.setJwks(jwksMap);
        }

        // TokenSettings fields
        OAuth2TokenSettings ts = new OAuth2TokenSettings();
        ts.setAuthorizationCodeTimeToLive(entity.getAuthorizationCodeTimeToLive());
        ts.setAccessTokenTimeToLive(entity.getAccessTokenTimeToLive());
        ts.setAccessTokenFormat(entity.getAccessTokenFormat());
        ts.setDeviceCodeTimeToLive(entity.getDeviceCodeTimeToLive());
        ts.setReuseRefreshTokens(entity.getReuseRefreshTokens());
        ts.setRefreshTokenTimeToLive(entity.getRefreshTokenTimeToLive());
        model.setTokenSettings(ts);

        // Promoted top-level fields (OIDC Dynamic Registration / RFC 8705)
        model.setIdTokenSignedResponseAlgorithm(entity.getIdTokenSignatureAlgorithm());
        model.setTlsClientCertificateBoundAccessTokens(entity.getX509CertificateBoundAccessTokens());

        return model;
    }

    /**
     * Converts an {@link OAuth2Client} model to an {@link OAuth2ClientEntity}.
     *
     * @param model the model to convert
     * @return the entity, or {@code null} if the model is {@code null}
     */
    public static OAuth2ClientEntity toOAuth2ClientEntity(OAuth2Client model) {
        if (model == null) {
            return null;
        }

        OAuth2ClientEntity entity = new OAuth2ClientEntity();

        entity.setId(model.getRegistrationId());
        entity.setClientId(model.getClientId());
        entity.setClientIdIssuedAt(model.getClientIdIssuedAt());
        entity.setClientSecret(model.getClientSecret());
        entity.setClientSecretExpiresAt(model.getClientSecretExpiresAt());
        entity.setClientName(model.getClientName());

        // token_endpoint_auth_method: direct mapping
        entity.setTokenEndpointAuthMethod(model.getTokenEndpointAuthMethod());

        // grant_types -> comma-separated
        entity.setAuthorizationGrantTypes(
                setToCommaDelimited(model.getGrantTypes()));

        // response_types -> comma-separated
        entity.setResponseTypes(
                setToCommaDelimited(model.getResponseTypes()));

        entity.setRedirectUris(setToCommaDelimited(model.getRedirectUris()));
        entity.setPostLogoutRedirectUris(setToCommaDelimited(model.getPostLogoutRedirectUris()));
        entity.setScopes(setToCommaDelimited(model.getScopes()));

        // ClientSettings fields
        OAuth2ClientSettings mcs = (OAuth2ClientSettings) model.getClientSettings();
        if (mcs != null) {
            entity.setRequireProofKey(mcs.getRequireProofKey());
            entity.setRequireAuthorizationConsent(mcs.getRequireAuthorizationConsent());
        }
        entity.setJwkSetUrl(model.getJwksUri());

        // Promoted top-level fields (OIDC Dynamic Registration / RFC 8705)
        entity.setTokenEndpointAuthenticationSigningAlgorithm(model.getTokenEndpointAuthSigningAlgorithm());
        entity.setX509CertificateSubjectDN(model.getTlsClientAuthSubjectDN());

        // jwks: Map<String, Object> -> JSON string
        Map<String, Object> jwksMap = model.getJwks();
        if (jwksMap != null && !jwksMap.isEmpty()) {
            entity.setJwks(JacksonUtils.writeValueAsString(jwksMap));
        }

        // TokenSettings fields
        OAuth2TokenSettings mts = model.getTokenSettings();
        if (mts != null) {
            entity.setAuthorizationCodeTimeToLive(mts.getAuthorizationCodeTimeToLive());
            entity.setAccessTokenTimeToLive(mts.getAccessTokenTimeToLive());
            entity.setAccessTokenFormat(mts.getAccessTokenFormat());
            entity.setDeviceCodeTimeToLive(mts.getDeviceCodeTimeToLive());
            entity.setReuseRefreshTokens(mts.getReuseRefreshTokens());
            entity.setRefreshTokenTimeToLive(mts.getRefreshTokenTimeToLive());
        }

        // Promoted top-level fields (OIDC Dynamic Registration / RFC 8705)
        entity.setIdTokenSignatureAlgorithm(model.getIdTokenSignedResponseAlgorithm());
        entity.setX509CertificateBoundAccessTokens(model.getTlsClientCertificateBoundAccessTokens());

        AuditingEntityUtils.updateAuditingEntity(model, entity);

        return entity;
    }

    private static Set<String> commaDelimitedToSet(String str) {
        if (!StringUtils.hasText(str)) {
            return Collections.emptySet();
        }
        return StringUtils.commaDelimitedListToSet(str);
    }

    private static String setToCommaDelimited(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        return String.join(",", set);
    }
}
