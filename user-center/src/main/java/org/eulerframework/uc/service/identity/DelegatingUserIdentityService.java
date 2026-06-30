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
package org.eulerframework.uc.service.identity;

import org.eulerframework.security.core.identity.*;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Composite {@link UserIdentityService} that routes per-type to a
 * registry of backend implementations keyed by
 * {@link UserIdentityService#identityType()}.
 *
 * <p>Wired as the {@link org.springframework.context.annotation.Primary
 * primary} {@code UserIdentityService} bean in deployments that support
 * more than one identity type; per-type backends remain regular SPI
 * beans and are collected through the constructor.
 *
 * <p>Dispatch rules:
 * <ul>
 *   <li>{@link #createUserIdentity(String, MultiValueMap)} reads
 *       {@link #IDENTITY_TYPE_PARAMETER} from the form parameters.</li>
 *   <li>{@link #createUserIdentity(String, UserIdentity)} reads
 *       {@link UserIdentity#getIdentityType()} from the prototype.</li>
 *   <li>{@link #findUserIdentityByRawSubject(String, String)} dispatches
 *       on the {@code identityType} argument.</li>
 *   <li>{@link #getUserIdentity(String, String)} and
 *       {@link #deleteUserIdentity(String, String)} fan out across
 *       every backend; per-SPI contract reads return
 *       {@link Optional#empty()} and deletes are silent for ids not
 *       owned by the backend, so at most one backend acts.</li>
 *   <li>{@link #listUserIdentities(String)} aggregates across all
 *       backends and orders by {@link UserIdentity#getBoundAt()}
 *       descending.</li>
 *   <li>{@link #updateUserIdentity(String, String, MultiValueMap)}
 *       resolves the owning backend via {@code getUserIdentity} and
 *       forwards.</li>
 * </ul>
 *
 * <p>{@link #identityType()} returns {@link #DELEGATING_IDENTITY_TYPE}.
 * The constructor refuses backends that claim this sentinel and
 * filters out any nested {@code DelegatingUserIdentityService} from the
 * routing table to guard against self-routing.
 *
 * @see UserIdentityService
 */
public class DelegatingUserIdentityService implements UserIdentityService {

    /**
     * Sentinel returned by {@link DelegatingUserIdentityService#identityType()}.
     * Per-type backends must never return this value. The delegator
     * filters it out of its routing table to prevent self-routing.
     */
    String DELEGATING_IDENTITY_TYPE = "__delegating__";

    private final Map<String, UserIdentityService> backends;

    public DelegatingUserIdentityService(UserIdentityService... backends) {
        Assert.notNull(backends, "backends must not be null");
        Map<String, UserIdentityService> registry = new LinkedHashMap<>(backends.length);
        for (UserIdentityService backend : backends) {
            String identityType = backend.identityType();
            Assert.hasText(identityType, "identityType must not be empty for backend "
                    + backend.getClass().getName());
            UserIdentityService previous = registry.putIfAbsent(identityType, backend);
            Assert.isNull(previous, () ->
                    "Duplicate UserIdentityService backend for identity_type '"
                            + identityType + "': "
                            + (previous != null ? previous.getClass().getName() : "")
                            + " and " + backend.getClass().getName());
        }
        this.backends = Collections.unmodifiableMap(registry);
    }

    @Override
    public String identityType() {
        return DELEGATING_IDENTITY_TYPE;
    }

    @Override
    public UserIdentity createUserIdentity(String userId, MultiValueMap<String, String> params) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(params, "params must not be null");
        String identityType = params.getFirst(IDENTITY_TYPE_PARAMETER);
        if (!StringUtils.hasText(identityType)) {
            throw new InvalidUserIdentityException(
                    "Form parameter '" + IDENTITY_TYPE_PARAMETER + "' is required");
        }
        return resolveBackend(identityType).createUserIdentity(userId, params);
    }

    @Override
    public UserIdentity createUserIdentity(String userId, UserIdentity prototype) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(prototype, "prototype must not be null");
        Assert.hasText(prototype.getIdentityType(), "identityType must not be empty");
        return resolveBackend(prototype.getIdentityType()).createUserIdentity(userId, prototype);
    }

    @Override
    public Optional<UserIdentity> getUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        // identityId is a globally unique UUID; at most one backend
        // returns non-empty.
        for (UserIdentityService backend : this.backends.values()) {
            Optional<UserIdentity> identity = backend.getUserIdentity(userId, identityId);
            if (identity.isPresent()) {
                return identity;
            }
        }
        return Optional.empty();
    }

    @Override
    public List<UserIdentity> listUserIdentities(String userId) {
        return listUserIdentities(userId, null);
    }

    @Override
    public List<UserIdentity> listUserIdentities(String userId, String identityType) {
        Assert.hasText(userId, "userId must not be empty");
        if (StringUtils.hasText(identityType)) {
            UserIdentityService backend = this.backends.get(identityType);
            if (backend == null) {
                return List.of();
            }
            return backend.listUserIdentities(userId);
        }
        return this.backends.values().stream()
                .flatMap(backend -> backend.listUserIdentities(userId).stream())
                // Most recently bound first.
                .sorted(Comparator.comparing(UserIdentity::getBoundAt).reversed())
                .toList();
    }

    @Override
    public UserIdentity updateUserIdentity(String userId, String identityId,
                                           MultiValueMap<String, String> params) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Assert.notNull(params, "params must not be null");
        UserIdentity existing = getUserIdentity(userId, identityId)
                .orElseThrow(() -> new UserIdentityNotFoundException(identityId));
        return resolveBackend(existing.getIdentityType())
                .updateUserIdentity(userId, identityId, params);
    }

    @Override
    public UserIdentity updateUserIdentity(String userId, String identityId, UserIdentity prototype) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Assert.notNull(prototype, "prototype must not be null");
        UserIdentity existing = getUserIdentity(userId, identityId)
                .orElseThrow(() -> new UserIdentityNotFoundException(identityId));
        return resolveBackend(existing.getIdentityType())
                .updateUserIdentity(userId, identityId, prototype);
    }

    @Override
    public void deleteUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        // Per SPI contract each backend silently ignores ids it does
        // not own; fan-out delete is idempotent.
        for (UserIdentityService backend : this.backends.values()) {
            backend.deleteUserIdentity(userId, identityId);
        }
    }

    @Override
    public Optional<UserIdentity> findUserIdentityByRawSubject(String identityType, String rawSubject) {
        if (!StringUtils.hasText(identityType) || !StringUtils.hasText(rawSubject)) {
            return Optional.empty();
        }
        UserIdentityService backend = this.backends.get(identityType);
        if (backend == null) {
            return Optional.empty();
        }
        return backend.findUserIdentityByRawSubject(identityType, rawSubject);
    }

    @Override
    public Optional<String> getRawFieldValue(String userId, String identityId, String fieldName) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Assert.hasText(fieldName, "fieldName must not be empty");
        for (UserIdentityService backend : this.backends.values()) {
            Optional<String> raw = backend.getRawFieldValue(userId, identityId, fieldName);
            if (raw.isPresent()) {
                return raw;
            }
        }
        return Optional.empty();
    }

    private UserIdentityService resolveBackend(String identityType) {
        UserIdentityService backend = this.backends.get(identityType);
        if (backend == null) {
            throw new UnsupportedIdentityTypeException(identityType);
        }
        return backend;
    }
}
