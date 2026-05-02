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
package org.eulerframework.uc.appattest.util;

import org.eulerframework.security.authentication.appattest.AppAttestApp;
import org.eulerframework.security.authentication.appattest.AppAttestUtils;
import org.eulerframework.security.authentication.appattest.DefaultAppAttestApp;
import org.eulerframework.security.authentication.appattest.RegisteredApp;
import org.eulerframework.uc.appattest.entity.AppEntity;

/**
 * Conversion helpers between {@link AppEntity} and {@link AppAttestApp}.
 *
 * <p>This class only deals with the Entity &harr; service-layer model mapping.
 * The {@code RegisteredApp} &harr; {@code AppAttestApp} mapping is already
 * provided by the framework layer ({@link DefaultAppAttestApp#reloadRegisteredApp}
 * and the bridge repository's internal {@code toRegisteredApp}).
 */
public abstract class AppModelUtils {

    private AppModelUtils() {
        // utility class
    }

    /**
     * Converts an {@link AppEntity} to a {@link DefaultAppAttestApp} model.
     *
     * @param entity the entity to convert
     * @return the model, or {@code null} if the entity is {@code null}
     */
    public static DefaultAppAttestApp toAppAttestApp(AppEntity entity) {
        if (entity == null) {
            return null;
        }
        DefaultAppAttestApp model = new DefaultAppAttestApp();
        model.setRegistrationId(entity.getId());
        model.setTeamId(entity.getTeamId());
        model.setBundleId(entity.getBundleId());
        model.setOauth2Enabled(entity.isOauth2Enabled());
        model.setOauth2ClientType(entity.getOauth2ClientType());
        return model;
    }

    /**
     * Converts an {@link AppAttestApp} to a new {@link AppEntity}.
     *
     * <p>The primary key column {@code id} is populated from
     * {@link AppAttestApp#getRegistrationId() registrationId}; the {@code appId}
     * column stores {@link AppAttestApp#getAppId() appId} verbatim; the
     * {@code appIdHash} column stores the hex-encoded SHA-256 digest of
     * {@code appId} computed via {@link AppAttestUtils#appIdHashHex(String)}.
     *
     * <p>Intended for the {@code createApp} path where {@code model} is expected
     * to carry a fully-populated state. {@link AppAttestApp#getOauth2Enabled()
     * oauth2Enabled} is coerced to {@code false} when {@code null}.
     *
     * @param model the model to convert
     * @return the entity, or {@code null} if the model is {@code null}
     */
    public static AppEntity toAppEntity(AppAttestApp model) {
        if (model == null) {
            return null;
        }
        AppEntity entity = new AppEntity();
        entity.setId(model.getRegistrationId());
        entity.setAppId(model.getAppId());
        entity.setAppIdHash(AppAttestUtils.appIdHashHex(model.getAppId()));
        entity.setTeamId(model.getTeamId());
        entity.setBundleId(model.getBundleId());
        entity.setOauth2Enabled(Boolean.TRUE.equals(model.getOauth2Enabled()));
        entity.setOauth2ClientType(model.getOauth2ClientType());
        return entity;
    }

    /**
     * Patch-style update of {@code target} from {@code src}: each field of
     * {@code src} is applied only when it is non-{@code null}.
     *
     * <p>{@link AppAttestApp#getTeamId() teamId} and
     * {@link AppAttestApp#getBundleId() bundleId} travel together &mdash; the
     * pairing invariant is enforced by {@link AppAttestApp#getAppId()} &mdash;
     * so they are written as one atomic block along with the recomputed
     * {@code appId} / {@code appIdHash}. {@link AppAttestApp#getOauth2Enabled()
     * oauth2Enabled} of {@code null} means "unchanged".
     *
     * <p>Full-overwrite semantics &mdash; required by the HTTP {@code PUT} path
     * and by the {@code RegisteredApp} bridge &mdash; use
     * {@link #replaceAppEntity(AppAttestApp, AppEntity)} instead, which is
     * the mirror image of this method.
     *
     * @param src    the source model
     * @param target the target entity to mutate
     */
    public static void patchAppEntity(AppAttestApp src, AppEntity target) {
        if (src == null || target == null) {
            return;
        }
        String appId = src.getAppId();
        if (appId != null) {
            target.setAppId(appId);
            target.setAppIdHash(AppAttestUtils.appIdHashHex(appId));
            target.setTeamId(src.getTeamId());
            target.setBundleId(src.getBundleId());
        }
        Boolean oauth2Enabled = src.getOauth2Enabled();
        if (oauth2Enabled != null) {
            target.setOauth2Enabled(oauth2Enabled);
        }
        RegisteredApp.OAuth2ClientType oauth2ClientType = src.getOauth2ClientType();
        if (oauth2ClientType != null) {
            target.setOauth2ClientType(oauth2ClientType);
        }
    }

    /**
     * Copies every mapped field from {@code model} onto {@code entity} using
     * full-overwrite semantics: {@code null} values on the model overwrite the
     * corresponding entity state (the mirror image of
     * {@link #patchAppEntity(AppAttestApp, AppEntity)}, which uses patch
     * semantics).
     *
     * <p>{@code appIdHash} is always recomputed from {@link AppAttestApp#getAppId()}
     * to keep the two columns in sync. {@link AppAttestApp#getOauth2Enabled()
     * oauth2Enabled} of {@code null} is coerced to {@code false} because the
     * entity column is a primitive {@code boolean}.
     *
     * @param model  the source model; when {@code null} the method returns
     *               without touching the entity
     * @param entity the target entity to overwrite
     */
    public static void replaceAppEntity(AppAttestApp model, AppEntity entity) {
        if (model == null) {
            return;
        }
        entity.setId(model.getRegistrationId());
        String appId = model.getAppId();
        entity.setAppId(appId);
        entity.setAppIdHash(appId == null ? null : AppAttestUtils.appIdHashHex(appId));
        entity.setTeamId(model.getTeamId());
        entity.setBundleId(model.getBundleId());
        entity.setOauth2Enabled(Boolean.TRUE.equals(model.getOauth2Enabled()));
        entity.setOauth2ClientType(model.getOauth2ClientType());
    }
}
