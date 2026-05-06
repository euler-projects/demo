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

    @Override
    @Transactional
    public JwkModel createJwk(JwkEntry entry) {
        Assert.notNull(entry, "entry is required");
        ManagedJwk managedJwk = new JwkModel();
        managedJwk.reloadJwkEntry(entry);
        return this.createJwk(managedJwk);
    }

    @Override
    @Transactional
    public JwkModel createJwk(ManagedJwk managedJwk) {
        Assert.notNull(managedJwk, "managedJwk is required");
        JwkEntity entity = OAuth2JwkEntryModelUtils.toJwkEntity(managedJwk);
        JwkEntity saved = this.jwkEntityRepository.save(entity);
        return OAuth2JwkEntryModelUtils.toJwkModel(saved);
    }

    @Override
    public JwkModel getJwk(String kid) {
        Assert.hasText(kid, "key is required");
        return this.jwkEntityRepository.findById(kid)
                .map(OAuth2JwkEntryModelUtils::toJwkModel)
                .orElse(null);
    }

    @Override
    public List<ManagedJwk> listJwks() {
        return this.jwkEntityRepository.findAll()
                .stream()
                .map(OAuth2JwkEntryModelUtils::toJwkModel)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @Transactional
    public void updateJwk(JwkEntry entry) {
        Assert.notNull(entry, "entry is required");
        ManagedJwk managedJwk = new JwkModel();
        managedJwk.reloadJwkEntry(entry);
        this.updateJwk(managedJwk);
    }

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

    @Override
    @Transactional
    public void deleteJwk(String kid) {
        if (!this.jwkEntityRepository.existsById(kid)) {
            throw new ResourceNotFoundException("JWK not found, kid: " + kid);
        }

        this.jwkEntityRepository.deleteById(kid);
    }

    @Override
    public String getFingerprint() {
        List<JwkEntity> entities = this.jwkEntryDao.listForCalcFingerprint();
        return OAuth2JwkEntryModelUtils.fingerprint(entities);
    }
}
