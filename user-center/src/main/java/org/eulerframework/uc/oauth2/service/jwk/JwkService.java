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

package org.eulerframework.uc.oauth2.service.jwk;

import org.eulerframework.security.jwk.JwkEntry;
import org.eulerframework.security.jwk.JwkManageService;
import org.eulerframework.security.jwk.ManagedJwk;
import org.eulerframework.uc.oauth2.dao.JwkEntryDao;
import org.eulerframework.uc.oauth2.entity.JwkEntity;
import org.eulerframework.uc.oauth2.model.JwkModel;
import org.eulerframework.uc.oauth2.repository.JwkEntityRepository;
import org.eulerframework.uc.oauth2.util.OAuth2JwkEntryModelUtils;
import org.eulerframework.web.core.exception.web.api.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;


/**
 * JPA-backed {@link JwkManageService} used by the user-center when the
 * {@code persistent-jwk-source} Spring profile is active. All JWKs are stored in
 * the {@code oauth2_jwk} table through {@link JwkEntityRepository}, with the
 * private material encrypted at rest by {@code JwkAttributeConverter}.
 * <p>
 * The service translates between {@link JwkEntry} / {@link ManagedJwk} (framework
 * contracts) and {@link JwkEntity} (persistence contract) via
 * {@link OAuth2JwkEntryModelUtils}. It never exposes raw entities to callers.
 */
@Service
@Profile("persistent-jwk-source")
public class JwkService implements JwkManageService {

    private JwkEntityRepository jwkEntityRepository;
    private JwkEntryDao jwkEntryDao;

    @Autowired
    public void setJwkEntityRepository(JwkEntityRepository jwkEntityRepository) {
        this.jwkEntityRepository = jwkEntityRepository;
    }

    @Autowired
    public void setJwkEntryDao(JwkEntryDao jwkEntryDao) {
        this.jwkEntryDao = jwkEntryDao;
    }

    /**
     * Wrap the given {@link JwkEntry} in a new {@link JwkModel} and persist it.
     *
     * @param entry freshly generated JWK entry; must not be {@code null}
     * @return the persisted JWK as a {@link JwkModel}
     */
    @Override
    @Transactional
    public JwkModel createJwk(JwkEntry entry) {
        Assert.notNull(entry, "entry is required");
        ManagedJwk managedJwk = new JwkModel();
        managedJwk.reloadJwkEntry(entry);
        return this.createJwk(managedJwk);
    }

    /**
     * Persist the supplied {@link ManagedJwk} as a brand-new row.
     *
     * @param managedJwk JWK to insert; must not be {@code null}
     * @return the persisted JWK as a {@link JwkModel}
     */
    @Override
    @Transactional
    public JwkModel createJwk(ManagedJwk managedJwk) {
        Assert.notNull(managedJwk, "managedJwk is required");
        JwkEntity entity = OAuth2JwkEntryModelUtils.toJwkEntity(managedJwk);
        JwkEntity saved = this.jwkEntityRepository.save(entity);
        return OAuth2JwkEntryModelUtils.toJwkModel(saved);
    }

    /**
     * Look up a JWK by its {@code kid}.
     *
     * @param kid non-blank key identifier
     * @return the matching {@link JwkModel}, or {@code null} when not found
     */
    @Override
    public JwkModel getJwk(String kid) {
        Assert.hasText(kid, "key is required");
        return this.jwkEntityRepository.findById(kid)
                .map(OAuth2JwkEntryModelUtils::toJwkModel)
                .orElse(null);
    }

    /**
     * List every persisted JWK.
     *
     * @return an unmodifiable, possibly empty snapshot
     */
    @Override
    public List<ManagedJwk> listJwks() {
        return this.jwkEntityRepository.findAll()
                .stream()
                .map(OAuth2JwkEntryModelUtils::toJwkModel)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Replace an existing JWK with the data carried by {@code entry}.
     *
     * @param entry replacement payload; must not be {@code null}
     */
    @Override
    @Transactional
    public void updateJwk(JwkEntry entry) {
        Assert.notNull(entry, "entry is required");
        ManagedJwk managedJwk = new JwkModel();
        managedJwk.reloadJwkEntry(entry);
        this.updateJwk(managedJwk);
    }

    /**
     * Replace an existing JWK with the state carried by {@code managedJwk}.
     * All of {@code kid}, {@code jwk} and {@code status} must be present.
     *
     * @param managedJwk replacement state; must not be {@code null}
     * @throws ResourceNotFoundException if no row matches {@code managedJwk.kid}
     */
    @Override
    @Transactional
    public void updateJwk(ManagedJwk managedJwk) {
        Assert.notNull(managedJwk, "managedJwk is required");
        Assert.hasText(managedJwk.getKid(), "managedJwk.kid is required");
        Assert.notNull(managedJwk.getJwk(), "managedJwk.jwk is required");
        Assert.notNull(managedJwk.getStatus(), "managedJwk.status is required");
        JwkEntity existsEntity = this.jwkEntityRepository.findById(managedJwk.getKid())
                .orElseThrow(() -> new ResourceNotFoundException("JWK not found, kid: " + managedJwk.getKid()));
        OAuth2JwkEntryModelUtils.updateJwkEntity(managedJwk, existsEntity);
        this.jwkEntityRepository.save(existsEntity);
    }

    /**
     * Apply a partial update to an existing JWK. Only the non-{@code null}
     * properties of {@code managedJwk} overwrite the persisted values; the
     * {@code kid} is mandatory as the lookup key.
     *
     * @param managedJwk patch payload; must not be {@code null}
     * @throws ResourceNotFoundException if no row matches {@code managedJwk.kid}
     */
    @Override
    @Transactional
    public void patchJwk(ManagedJwk managedJwk) {
        Assert.notNull(managedJwk, "managedJwk is required");
        Assert.hasText(managedJwk.getKid(), "managedJwk.kid is required");
        JwkEntity existsEntity = this.jwkEntityRepository.findById(managedJwk.getKid())
                .orElseThrow(() -> new ResourceNotFoundException("JWK not found, kid: " + managedJwk.getKid()));
        OAuth2JwkEntryModelUtils.patchJwkEntity(managedJwk, existsEntity);
        this.jwkEntityRepository.save(existsEntity);
    }

    /**
     * Delete the JWK identified by {@code kid}.
     *
     * @param kid key identifier to delete
     * @throws ResourceNotFoundException if no row matches {@code kid}
     */
    @Override
    @Transactional
    public void deleteJwk(String kid) {
        if (!this.jwkEntityRepository.existsById(kid)) {
            throw new ResourceNotFoundException("JWK not found, kid: " + kid);
        }

        this.jwkEntityRepository.deleteById(kid);
    }

    /**
     * Compute a stable fingerprint over all stored JWKs. The DAO projects only
     * the columns needed for the digest (kid, iat, status, fingerprint, audit
     * timestamps) so the JWK JSON ciphertext never leaves the database.
     *
     * @return a Base64URL-encoded SHA-256 digest that only changes when the JWK
     *         set (or individual fingerprint bytes) change
     */
    @Override
    public String getFingerprint() {
        List<JwkEntity> entities = this.jwkEntryDao.listForCalcFingerprint();
        return OAuth2JwkEntryModelUtils.fingerprint(entities);
    }
}
