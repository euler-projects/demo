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
package org.eulerframework.uc.service;

import org.eulerframework.security.authentication.factor.InvalidAuthenticationFactorRequestException;
import org.eulerframework.security.authentication.factor.UnsupportedFactorTypeException;
import org.eulerframework.security.authentication.factor.UserAuthenticationFactor;
import org.eulerframework.security.authentication.factor.UserAuthenticationFactorNotFoundException;
import org.eulerframework.security.authentication.factor.UserAuthenticationFactorService;
import org.eulerframework.uc.entity.UserAuthenticationFactorEntity;
import org.eulerframework.uc.repository.UserAuthenticationFactorRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single {@link UserAuthenticationFactorService} bean exposed by user-center
 * that the framework's {@code /user/identities} endpoint filter delegates
 * to.
 * <p>
 * Routing strategy is driven by the parent table {@code t_user_authentication_factor},
 * which already carries the {@code factor_type} discriminator alongside
 * {@code user_id} and {@code factor_id}. Compared to a fan-out over every backend
 * service, this avoids querying every per-factor child table when the
 * caller only addresses one factor:
 * <ul>
 *     <li>{@link #bind} dispatches purely on the {@code factor_type} form
 *         parameter \u2014 no table read needed.</li>
 *     <li>{@link #findById} performs a single lookup on the parent table to
 *         determine the owning factor type, then delegates to that backend.</li>
 *     <li>{@link #deleteById} mirrors {@link #findById}: parent-table lookup
 *         + targeted backend dispatch, no fan-out.</li>
 *     <li>{@link #findAllByUserId} aggregates the result across all
 *         registered backends. We could narrow the set by reading
 *         {@code distinct factor_type} for the user from the parent table
 *         first, but the win is marginal when only a handful of factor
 *         types are configured.</li>
 * </ul>
 * <p>
 * Backends are collected from the application context via constructor
 * injection of {@code List<UserAuthenticationFactorService>}; Spring
 * automatically excludes the bean currently being created from the
 * collection, so this composite cannot recursively route to itself. As an
 * additional defensive measure we also filter by type in the constructor.
 */
@Service
@Primary
public class CompositeUserAuthenticationFactorService implements UserAuthenticationFactorService {

    /**
     * Sentinel value returned by {@link #factorType()}. It is not a
     * client-visible factor type and never participates in routing because
     * the composite excludes itself from the route table.
     */
    public static final String COMPOSITE_FACTOR_TYPE = "__composite__";

    private final UserAuthenticationFactorRepository factorRepository;
    private final Map<String, UserAuthenticationFactorService> routes;

    public CompositeUserAuthenticationFactorService(UserAuthenticationFactorRepository factorRepository,
                                                    List<UserAuthenticationFactorService> backends) {
        Assert.notNull(factorRepository, "factorRepository is required");
        Assert.notNull(backends, "backends must not be null");
        this.factorRepository = factorRepository;
        Map<String, UserAuthenticationFactorService> routeMap = new LinkedHashMap<>(backends.size());
        for (UserAuthenticationFactorService backend : backends) {
            if (backend instanceof CompositeUserAuthenticationFactorService) {
                continue;
            }
            String factorType = backend.factorType();
            Assert.hasText(factorType,
                    "factorType must not be empty for backend " + backend.getClass().getName());
            UserAuthenticationFactorService previous = routeMap.putIfAbsent(factorType, backend);
            Assert.isNull(previous, () -> "Duplicate UserAuthenticationFactorService backend for factorType '"
                    + factorType + "': "
                    + (previous != null ? previous.getClass().getName() : "")
                    + " and " + backend.getClass().getName());
        }
        this.routes = Collections.unmodifiableMap(routeMap);
    }

    @Override
    public String factorType() {
        return COMPOSITE_FACTOR_TYPE;
    }

    @Override
    public UserAuthenticationFactor bind(String userId, MultiValueMap<String, String> params) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(params, "params must not be null");
        String factorType = params.getFirst(FACTOR_TYPE_PARAMETER);
        if (!StringUtils.hasText(factorType)) {
            throw new InvalidAuthenticationFactorRequestException(
                    "Form parameter '" + FACTOR_TYPE_PARAMETER + "' is required");
        }
        return resolveBackend(factorType).bind(userId, params);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthenticationFactor> findById(String userId, String factorId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(factorId, "factorId must not be empty");
        Optional<UserAuthenticationFactorEntity> entity = this.factorRepository.findByIdAndUserId(factorId, userId);
        if (entity.isEmpty()) {
            return Optional.empty();
        }
        UserAuthenticationFactorService backend = this.routes.get(entity.get().getFactorType());
        if (backend == null) {
            // Persisted factor whose backend is no longer configured —
            // surface as not-found rather than 500 so the client can move on.
            return Optional.empty();
        }
        return backend.findById(userId, factorId);
    }

    @Override
    public List<UserAuthenticationFactor> findAllByUserId(String userId) {
        Assert.hasText(userId, "userId must not be empty");
        return this.routes.values().stream()
                .flatMap(backend -> backend.findAllByUserId(userId).stream())
                .toList();
    }

    /**
     * Routes the lookup precisely on {@code factorType} — we never fan-out
     * across backends here. Fan-out would risk cross-namespace false
     * positives (e.g. a SHA-256 hex emitted by the {@code phone} backend
     * accidentally collides with a WeChat openid space) and would also
     * defeat the whole point of carrying {@code factorType} on the SPI.
     * Unknown factor types simply return {@link Optional#empty()} — this
     * is a query, not a mutation, so {@link UnsupportedFactorTypeException}
     * would be too loud.
     */
    @Override
    public Optional<UserAuthenticationFactor> findByOriginalIdentifier(
            String factorType, String originalIdentifier) {
        if (!StringUtils.hasText(factorType) || !StringUtils.hasText(originalIdentifier)) {
            return Optional.empty();
        }
        UserAuthenticationFactorService backend = this.routes.get(factorType);
        if (backend == null) {
            return Optional.empty();
        }
        return backend.findByOriginalIdentifier(factorType, originalIdentifier);
    }

    @Override
    public UserAuthenticationFactor bindOriginalIdentifier(
            String userId, String factorType, String originalIdentifier) {
        Assert.hasText(userId, "userId must not be empty");
        if (!StringUtils.hasText(factorType)) {
            throw new InvalidAuthenticationFactorRequestException(
                    "factor type is required");
        }
        return resolveBackend(factorType).bindOriginalIdentifier(userId, factorType, originalIdentifier);
    }

    @Override
    @Transactional
    public UserAuthenticationFactor update(String userId, String factorId, MultiValueMap<String, String> params) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(factorId, "factorId must not be empty");
        Assert.notNull(params, "params must not be null");
        UserAuthenticationFactorEntity entity = this.factorRepository.findByIdAndUserId(factorId, userId)
                .orElseThrow(() -> new UserAuthenticationFactorNotFoundException(factorId));
        UserAuthenticationFactorService backend = this.routes.get(entity.getFactorType());
        if (backend == null) {
            throw new UnsupportedFactorTypeException(entity.getFactorType());
        }
        return backend.update(userId, factorId, params);
    }

    @Override
    public void deleteById(String userId, String factorId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(factorId, "factorId must not be empty");
        UserAuthenticationFactorEntity entity = this.factorRepository.findByIdAndUserId(factorId, userId)
                .orElseThrow(() -> new UserAuthenticationFactorNotFoundException(factorId));
        UserAuthenticationFactorService backend = this.routes.get(entity.getFactorType());
        if (backend == null) {
            throw new UnsupportedFactorTypeException(entity.getFactorType());
        }
        backend.deleteById(userId, factorId);
    }

    private UserAuthenticationFactorService resolveBackend(String factorType) {
        UserAuthenticationFactorService backend = this.routes.get(factorType);
        if (backend == null) {
            throw new UnsupportedFactorTypeException(factorType);
        }
        return backend;
    }
}
