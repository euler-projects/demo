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
        entity.setOauth2Enabled(model.isOauth2Enabled());
        entity.setOauth2ClientType(model.getOauth2ClientType());
        return entity;
    }

    /**
     * Full-overwrite update of {@code target} from {@code src} on all non-audit,
     * non-primary-key fields. Used by the update path.
     *
     * @param src    the source model
     * @param target the target entity to mutate
     */
    public static void updateAppEntity(AppAttestApp src, AppEntity target) {
        if (src == null || target == null) {
            return;
        }
        target.setAppId(src.getAppId());
        target.setAppIdHash(AppAttestUtils.appIdHashHex(src.getAppId()));
        target.setTeamId(src.getTeamId());
        target.setBundleId(src.getBundleId());
        target.setOauth2Enabled(src.isOauth2Enabled());
        target.setOauth2ClientType(src.getOauth2ClientType());
    }
}
