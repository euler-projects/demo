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

package org.eulerframework.uc.oauth2.util;

import com.nimbusds.jose.jwk.JWKSet;
import org.eulerframework.security.oauth2.server.authorization.client.DefaultEulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.client.EulerOAuth2Client;
import org.eulerframework.security.oauth2.server.authorization.settings.EulerOAuth2ClientSettings;
import org.eulerframework.security.oauth2.server.authorization.settings.EulerOAuth2TokenSettings;
import org.eulerframework.uc.oauth2.entity.OAuth2ClientEntity;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.*;

public abstract class OAuth2ClientModelUtils {

    /**
     * Converts an {@link OAuth2ClientEntity} to an {@link DefaultEulerOAuth2Client} model.
     *
     * @param entity the entity to convert
     * @return the model, or {@code null} if the entity is {@code null}
     */
    public static DefaultEulerOAuth2Client toEulerOAuth2Client(OAuth2ClientEntity entity) {
        if (entity == null) {
            return null;
        }

        DefaultEulerOAuth2Client model = new DefaultEulerOAuth2Client();

        model.setRegistrationId(entity.getId());
        model.setClientId(entity.getClientId());
        model.setClientIdIssuedAt(entity.getClientIdIssuedAt());
        model.setClientSecret(entity.getClientSecret());
        model.setClientSecretExpiresAt(entity.getClientSecretExpiresAt());
        model.setClientName(entity.getClientName());
        model.setTokenEndpointAuthMethod(entity.getTokenEndpointAuthMethod());
        model.setGrantTypes(commaDelimitedToSet(entity.getGrantTypes()));
        model.setResponseTypes(commaDelimitedToSet(entity.getResponseTypes()));
        model.setRedirectUris(commaDelimitedToSet(entity.getRedirectUris()));
        model.setPostLogoutRedirectUris(commaDelimitedToSet(entity.getPostLogoutRedirectUris()));
        model.setScopes(commaDelimitedToSet(entity.getScopes()));
        model.setJwksUri(entity.getJwkSetUrl());
        model.setJwks(OAuth2ClientModelUtils.parseJwks(entity.getJwks()));
        model.setTokenEndpointAuthSigningAlgorithm(entity.getTokenEndpointAuthSigningAlgorithm());
        model.setTlsClientAuthSubjectDN(entity.getTlsClientAuthSubjectDN());
        model.setIdTokenSignedResponseAlgorithm(entity.getIdTokenSignedResponseAlgorithm());
        model.setTlsClientCertificateBoundAccessTokens(entity.getTlsClientCertificateBoundAccessTokens());

        Map<String, Object> clientSettings = entity.getClientSettings();
        if (clientSettings != null) {
            model.setClientSettings(EulerOAuth2ClientSettings.withSettings(clientSettings).build());
        }
        Map<String, Object> tokenSettings = entity.getTokenSettings();
        if (tokenSettings != null) {
            model.setTokenSettings(EulerOAuth2TokenSettings.withSettings(tokenSettings).build());
        }
        return model;
    }

    /**
     * Converts an {@link DefaultEulerOAuth2Client} model to an {@link OAuth2ClientEntity}.
     *
     * @param model the model to convert
     * @return the entity, or {@code null} if the model is {@code null}
     */
    public static OAuth2ClientEntity toOAuth2ClientEntity(EulerOAuth2Client model) {
        if (model == null) {
            return null;
        }

        OAuth2ClientEntity entity = new OAuth2ClientEntity();
        patchOAuth2ClientEntity(model, entity);
        return entity;
    }

    /**
     * Patch-style update of {@code entity} from {@code model}: each field is
     * applied only when the incoming value is non-{@code null}. Mirrors HTTP
     * {@code PATCH} semantics.
     *
     * <p>Full-overwrite semantics &mdash; required by the HTTP {@code PUT} path
     * and by the {@code RegisteredClient} bridge &mdash; use
     * {@link #replaceOAuth2ClientEntity(EulerOAuth2Client, OAuth2ClientEntity)}
     * instead, which is the mirror image of this method.
     *
     * @param model  the source model
     * @param entity the target entity to mutate
     */
    public static void patchOAuth2ClientEntity(EulerOAuth2Client model, OAuth2ClientEntity entity) {
        if (model == null) {
            return;
        }
        Optional.ofNullable(model.getRegistrationId()).ifPresent(entity::setId);
        Optional.ofNullable(model.getClientId()).ifPresent(entity::setClientId);
        Optional.ofNullable(model.getClientIdIssuedAt()).ifPresent(entity::setClientIdIssuedAt);
        Optional.ofNullable(model.getClientSecret()).ifPresent(entity::setClientSecret);
        Optional.ofNullable(model.getClientSecretExpiresAt()).ifPresent(entity::setClientSecretExpiresAt);
        Optional.ofNullable(model.getClientName()).ifPresent(entity::setClientName);
        Optional.ofNullable(model.getTokenEndpointAuthMethod()).ifPresent(entity::setTokenEndpointAuthMethod);
        Optional.ofNullable(model.getGrantTypes()).map(OAuth2ClientModelUtils::setToCommaDelimited).ifPresent(entity::setGrantTypes);
        Optional.ofNullable(model.getResponseTypes()).map(OAuth2ClientModelUtils::setToCommaDelimited).ifPresent(entity::setResponseTypes);
        Optional.ofNullable(model.getRedirectUris()).map(OAuth2ClientModelUtils::setToCommaDelimited).ifPresent(entity::setRedirectUris);
        Optional.ofNullable(model.getPostLogoutRedirectUris()).map(OAuth2ClientModelUtils::setToCommaDelimited).ifPresent(entity::setPostLogoutRedirectUris);
        Optional.ofNullable(model.getScopes()).map(OAuth2ClientModelUtils::setToCommaDelimited).ifPresent(entity::setScopes);
        Optional.ofNullable(model.getJwksUri()).ifPresent(entity::setJwkSetUrl);
        Optional.ofNullable(model.getJwks()).map(JWKSet::toJSONObject).ifPresent(entity::setJwks);
        Optional.ofNullable(model.getTokenEndpointAuthSigningAlgorithm()).ifPresent(entity::setTokenEndpointAuthSigningAlgorithm);
        Optional.ofNullable(model.getIdTokenSignedResponseAlgorithm()).ifPresent(entity::setIdTokenSignedResponseAlgorithm);
        Optional.ofNullable(model.getTlsClientAuthSubjectDN()).ifPresent(entity::setTlsClientAuthSubjectDN);
        Optional.ofNullable(model.getTlsClientCertificateBoundAccessTokens()).ifPresent(entity::setTlsClientCertificateBoundAccessTokens);
        Optional.ofNullable(model.getClientSettings()).map(EulerOAuth2ClientSettings::getSettings).ifPresent(entity::setClientSettings);
        Optional.ofNullable(model.getTokenSettings()).map(EulerOAuth2TokenSettings::getSettings).ifPresent(entity::setTokenSettings);
    }

    /**
     * Copies every mapped field from {@code model} onto {@code entity} using
     * full-overwrite semantics: {@code null} values on the model overwrite the
     * corresponding entity state (the mirror image of
     * {@link #patchOAuth2ClientEntity(EulerOAuth2Client, OAuth2ClientEntity)},
     * which uses patch semantics).
     * <p>Three fields are treated as non-overwritable by {@code null} on the
     * replace path:
     * <ul>
     *   <li>{@code clientIdIssuedAt} &mdash; a lifecycle audit field; the
     *       original registration timestamp must not be wiped by a partially
     *       populated {@code RegisteredClient}.</li>
     *   <li>{@code clientSecret} / {@code clientSecretExpiresAt} &mdash;
     *       credential state owned exclusively by the rotation pipeline
     *       (see {@code updateClientSecret}); bulk replace paths such as the
     *       Admin {@code PUT} endpoint and the {@code RegisteredClient}
     *       bridge deliberately do not touch the credential.</li>
     * </ul>
     *
     * @param model  the source model; when {@code null} the method returns
     *               without touching the entity
     * @param entity the target entity to overwrite
     */
    public static void replaceOAuth2ClientEntity(EulerOAuth2Client model, OAuth2ClientEntity entity) {
        if (model == null) {
            return;
        }
        entity.setId(model.getRegistrationId());
        entity.setClientId(model.getClientId());
        if (model.getClientIdIssuedAt() != null) {
            entity.setClientIdIssuedAt(model.getClientIdIssuedAt());
        }
        if (model.getClientSecret() != null) {
            entity.setClientSecret(model.getClientSecret());
        }
        if (model.getClientSecretExpiresAt() != null) {
            entity.setClientSecretExpiresAt(model.getClientSecretExpiresAt());
        }
        entity.setClientName(model.getClientName());
        entity.setTokenEndpointAuthMethod(model.getTokenEndpointAuthMethod());
        entity.setGrantTypes(setToCommaDelimited(model.getGrantTypes()));
        entity.setResponseTypes(setToCommaDelimited(model.getResponseTypes()));
        entity.setRedirectUris(setToCommaDelimited(model.getRedirectUris()));
        entity.setPostLogoutRedirectUris(setToCommaDelimited(model.getPostLogoutRedirectUris()));
        entity.setScopes(setToCommaDelimited(model.getScopes()));
        entity.setJwkSetUrl(model.getJwksUri());
        entity.setJwks(model.getJwks() == null ? null : model.getJwks().toJSONObject());
        entity.setTokenEndpointAuthSigningAlgorithm(model.getTokenEndpointAuthSigningAlgorithm());
        entity.setIdTokenSignedResponseAlgorithm(model.getIdTokenSignedResponseAlgorithm());
        entity.setTlsClientAuthSubjectDN(model.getTlsClientAuthSubjectDN());
        entity.setTlsClientCertificateBoundAccessTokens(model.getTlsClientCertificateBoundAccessTokens());
        entity.setClientSettings(model.getClientSettings() == null ? null : model.getClientSettings().getSettings());
        entity.setTokenSettings(model.getTokenSettings() == null ? null : model.getTokenSettings().getSettings());
    }

    public static JWKSet parseJwks(Map<String, Object> jwks) {
        if (jwks == null) {
            return null;
        }
        try {
            return JWKSet.parse(jwks);
        } catch (ParseException e) {
            throw new IllegalStateException("JWKS data parse error: : " + e.getMessage(), e);
        }
    }

    public static JWKSet parseJwks(String jwks) {
        if (!StringUtils.hasText(jwks)) {
            return null;
        }
        try {
            return JWKSet.parse(jwks);
        } catch (ParseException e) {
            throw new IllegalStateException("JWKS data parse error: : " + e.getMessage(), e);
        }
    }

    private static Set<String> commaDelimitedToSet(String str) {
        if (!StringUtils.hasText(str)) {
            return Collections.emptySet();
        }
        return StringUtils.commaDelimitedListToSet(str);
    }

    private static String setToCommaDelimited(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        return String.join(",", collection);
    }
}
