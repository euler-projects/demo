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

import org.eulerframework.security.jwk.ManagedJwk;
import org.eulerframework.security.util.JwkUtils;
import org.eulerframework.uc.oauth2.entity.JwkEntity;
import org.eulerframework.uc.oauth2.model.JwkModel;

import java.util.*;

/**
 * Helpers that convert between the framework-level JWK contracts
 * ({@link ManagedJwk} and {@link JwkModel}) and the persistence contract
 * {@link JwkEntity}, and that compute stable per-entry / per-set fingerprints.
 */
public abstract class OAuth2JwkEntryModelUtils {
    /**
     * Translate a persisted {@link JwkEntity} into a {@link JwkModel} view.
     *
     * @param entity source entity; must not be {@code null}
     * @return freshly constructed {@link JwkModel}
     */
    public static JwkModel toJwkModel(JwkEntity entity) {
        JwkModel model = new JwkModel();
        model.setKid(entity.getKid());
        model.setStatus(entity.getStatus());
        model.setJwk(entity.getData());
        return model;
    }

    /**
     * Create a brand-new {@link JwkEntity} populated from {@code model}.
     *
     * @param model source model; must not be {@code null}
     * @return new entity ready to be inserted
     */
    public static JwkEntity toJwkEntity(ManagedJwk model) {
        JwkEntity entity = new JwkEntity();
        updateJwkEntity(model, entity);
        return entity;
    }

    /**
     * Overwrite every mutable field of {@code entity} with the state of {@code model}.
     * When the JWK is {@code null} the JWK-derived columns are cleared as well.
     *
     * @param model  source model
     * @param entity target entity to mutate
     */
    public static void updateJwkEntity(ManagedJwk model, JwkEntity entity) {
        entity.setKid(model.getKid());
        entity.setStatus(model.getStatus());

        if (model.getJwk() != null) {
            entity.setIat(model.getJwk().getIssueTime().toInstant());
            entity.setData(model.getJwk());
            entity.setFingerprint(JwkUtils.fingerprint(model.getJwk(), model.getStatus()));
        } else {
            entity.setIat(null);
            entity.setData(null);
            entity.setFingerprint(null);
        }
    }

    /**
     * Apply a partial update to {@code entity}: only the non-{@code null}
     * properties of {@code model} overwrite the corresponding columns. When
     * {@code model.jwk} is supplied, {@code iat}/{@code data}/{@code fingerprint}
     * are all refreshed together to keep them consistent.
     *
     * @param model  patch payload
     * @param entity target entity to mutate
     */
    public static void patchJwkEntity(ManagedJwk model, JwkEntity entity) {
        Optional.ofNullable(model.getKid()).ifPresent(entity::setKid);
        Optional.ofNullable(model.getStatus()).ifPresent(entity::setStatus);
        if (model.getJwk() != null) {
            entity.setIat(model.getJwk().getIssueTime().toInstant());
            entity.setData(model.getJwk());
            entity.setFingerprint(JwkUtils.fingerprint(model.getJwk(), model.getStatus()));
        }
    }


    /**
     * Aggregate the per-entry fingerprints of {@code entities} into a single
     * digest. Entries are sorted by {@code kid} first so the result is stable
     * regardless of storage iteration order.
     *
     * @param entities every persisted JWK entity to include; may be empty
     * @return Base64URL-encoded SHA-256 digest (or the empty-set marker when
     *         the input is empty)
     */
    public static String fingerprint(Collection<JwkEntity> entities) {
        List<byte[]> fingerprintOrderedByKid = entities.stream()
                .sorted(Comparator.comparing(JwkEntity::getKid))
                .map(JwkEntity::getFingerprint)
                .toList();

        return JwkUtils.hashFingerprints(fingerprintOrderedByKid);
    }

}
