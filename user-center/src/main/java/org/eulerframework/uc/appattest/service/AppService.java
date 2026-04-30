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
package org.eulerframework.uc.appattest.service;

import org.eulerframework.security.authentication.appattest.AppAttestApp;
import org.eulerframework.security.authentication.appattest.AppAttestAppService;
import org.eulerframework.security.authentication.appattest.DefaultAppAttestApp;
import org.eulerframework.security.authentication.appattest.RegisteredApp;
import org.eulerframework.security.authentication.appattest.RegisteredAppChangeListener;
import org.eulerframework.uc.appattest.entity.AppEntity;
import org.eulerframework.uc.appattest.repository.AppRepository;
import org.eulerframework.uc.appattest.util.AppModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppService implements AppAttestAppService {

    private AppRepository appRepository;

    /**
     * Listeners to notify after each successful write. Dispatched inline at the tail of
     * every mutating service method so that the callback runs inside the same
     * transactional boundary as the persistence call. The repository layer
     * ({@code AppAttestServiceRegisteredAppRepository}) treats dispatch as a
     * service-layer concern and does not wrap itself in any notification decorator.
     * Temporary until a cleaner notification strategy lands.
     */
    private List<RegisteredAppChangeListener> registeredAppChangeListeners = Collections.emptyList();

    @Override
    @Transactional
    public AppAttestApp createApp(AppAttestApp app) {
        Assert.notNull(app, "app must not be null");
        Assert.hasText(app.getTeamId(), "teamId must not be empty");
        Assert.hasText(app.getBundleId(), "bundleId must not be empty");
        String registrationId = StringUtils.hasText(app.getRegistrationId())
                ? app.getRegistrationId()
                : UUID.randomUUID().toString();
        if (this.appRepository.existsById(registrationId)) {
            throw new IllegalArgumentException(
                    "App already exists, registrationId: " + registrationId);
        }
        AppEntity entity = AppModelUtils.toAppEntity(app);
        entity.setId(registrationId);
        AppEntity saved = this.appRepository.save(entity);
        AppAttestApp persisted = AppModelUtils.toAppAttestApp(saved);
        notifyRegisteredAppSaved(persisted);
        return persisted;
    }

    @Override
    @Transactional
    public AppAttestApp createApp(RegisteredApp registeredApp) {
        Assert.notNull(registeredApp, "registeredApp must not be null");
        Assert.hasText(registeredApp.getId(), "registrationId must not be empty");
        Assert.hasText(registeredApp.getTeamId(), "teamId must not be empty");
        Assert.hasText(registeredApp.getBundleId(), "bundleId must not be empty");
        if (this.appRepository.existsById(registeredApp.getId())) {
            throw new IllegalArgumentException(
                    "App already exists, registrationId: " + registeredApp.getId());
        }
        DefaultAppAttestApp model = new DefaultAppAttestApp();
        model.reloadRegisteredApp(registeredApp);
        AppEntity entity = AppModelUtils.toAppEntity(model);
        AppEntity saved = this.appRepository.save(entity);
        AppAttestApp persisted = AppModelUtils.toAppAttestApp(saved);
        notifyRegisteredAppSaved(persisted);
        return persisted;
    }

    @Override
    public AppAttestApp findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId must not be empty");
        return this.appRepository.findById(registrationId)
                .map(AppModelUtils::toAppAttestApp)
                .orElse(null);
    }

    @Override
    public AppAttestApp findByAppIdHash(byte[] appIdHash) {
        Assert.notNull(appIdHash, "appIdHash must not be null");
        String hex = HexFormat.of().formatHex(appIdHash);
        return this.appRepository.findByAppIdHash(hex)
                .map(AppModelUtils::toAppAttestApp)
                .orElse(null);
    }

    @Override
    public AppAttestApp findByAppId(String appId) {
        Assert.hasText(appId, "appId must not be empty");
        return this.appRepository.findByAppId(appId)
                .map(AppModelUtils::toAppAttestApp)
                .orElse(null);
    }

    @Override
    @Transactional
    public void updateApp(AppAttestApp app) {
        Assert.notNull(app, "app must not be null");
        Assert.hasText(app.getRegistrationId(), "registrationId must not be empty");
        boolean teamIdPresent = StringUtils.hasText(app.getTeamId());
        boolean bundleIdPresent = StringUtils.hasText(app.getBundleId());
        Assert.isTrue(teamIdPresent == bundleIdPresent,
                "teamId and bundleId must be updated together: either both present or both absent");
        AppEntity entity = this.appRepository.findById(app.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "App not found, registrationId: " + app.getRegistrationId()));
        AppModelUtils.replaceAppEntity(app, entity);
        AppEntity saved = this.appRepository.save(entity);
        notifyRegisteredAppSaved(AppModelUtils.toAppAttestApp(saved));
    }

    @Override
    @Transactional
    public void updateApp(RegisteredApp registeredApp) {
        Assert.notNull(registeredApp, "registeredApp must not be null");
        Assert.hasText(registeredApp.getId(), "registrationId must not be empty");
        AppEntity entity = this.appRepository.findById(registeredApp.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "App not found, registrationId: " + registeredApp.getId()));
        DefaultAppAttestApp model = new DefaultAppAttestApp();
        model.reloadRegisteredApp(registeredApp);
        AppModelUtils.replaceAppEntity(model, entity);
        AppEntity saved = this.appRepository.save(entity);
        notifyRegisteredAppSaved(AppModelUtils.toAppAttestApp(saved));
    }

    @Override
    @Transactional
    public void patchApp(AppAttestApp app) {
        Assert.notNull(app, "app must not be null");
        Assert.hasText(app.getRegistrationId(), "registrationId must not be empty");
        boolean teamIdPresent = StringUtils.hasText(app.getTeamId());
        boolean bundleIdPresent = StringUtils.hasText(app.getBundleId());
        Assert.isTrue(teamIdPresent == bundleIdPresent,
                "teamId and bundleId must be patched together: either both present or both absent");
        AppEntity entity = this.appRepository.findById(app.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "App not found, registrationId: " + app.getRegistrationId()));
        AppModelUtils.patchAppEntity(app, entity);
        AppEntity saved = this.appRepository.save(entity);
        notifyRegisteredAppSaved(AppModelUtils.toAppAttestApp(saved));
    }

    @Override
    @Transactional
    public void deleteByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId must not be empty");
        this.appRepository.deleteById(registrationId);
    }

    @Override
    public List<AppAttestApp> listApps(int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        return this.appRepository.findAll(PageRequest.of(page, limit))
                .stream()
                .map(AppModelUtils::toAppAttestApp)
                .collect(Collectors.toList());
    }

    /**
     * Build a {@link RegisteredApp} view of {@code model} and fan it out to every
     * registered listener. teamId / bundleId are required here because the
     * {@link RegisteredApp.Builder} rejects blank values; all mutating entry points
     * above either validate them up front or reload them from the entity, so by the
     * time we reach this method the values are guaranteed non-blank.
     */
    private void notifyRegisteredAppSaved(AppAttestApp model) {
        if (this.registeredAppChangeListeners.isEmpty()) {
            return;
        }
        RegisteredApp view = RegisteredApp.withId(model.getRegistrationId())
                .teamId(model.getTeamId())
                .bundleId(model.getBundleId())
                .oauth2Enabled(Boolean.TRUE.equals(model.getOauth2Enabled()))
                .oauth2ClientType(model.getOauth2ClientType())
                .build();
        for (RegisteredAppChangeListener listener : this.registeredAppChangeListeners) {
            listener.onRegisteredAppSaved(view);
        }
    }

    @Autowired
    public void setAppRepository(AppRepository appRepository) {
        this.appRepository = appRepository;
    }

    @Autowired(required = false)
    public void setRegisteredAppChangeListeners(List<RegisteredAppChangeListener> listeners) {
        this.registeredAppChangeListeners = (listeners == null)
                ? Collections.emptyList()
                : List.copyOf(listeners);
    }
}
