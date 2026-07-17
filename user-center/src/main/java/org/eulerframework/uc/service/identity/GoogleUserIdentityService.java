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

import org.eulerframework.common.util.collections.MapUtils;
import org.eulerframework.security.core.identity.IdentityOccupiedException;
import org.eulerframework.security.core.identity.InvalidUserIdentityException;
import org.eulerframework.security.core.identity.UserIdentity;
import org.eulerframework.security.core.identity.UserIdentityNotFoundException;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.uc.entity.UserIdentityEntity;
import org.eulerframework.uc.entity.UserIdentityGoogleEntity;
import org.eulerframework.uc.repository.UserIdentityGoogleRepository;
import org.eulerframework.uc.repository.UserIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code google} backend of {@link UserIdentityService}.
 *
 * <p>Persists a parent {@code t_user_identity} row plus a child
 * {@code t_user_identity_google} row. The cross-account unique key is
 * the raw Google {@code sub} - the identity transform is the identity
 * function, matching the convention documented on
 * {@link UserIdentityService#findUserIdentityByRawSubject(String, String)}
 * for IdP-issued opaque values.
 *
 * <p>User creation via
 * {@link #createUserIdentity(String, MultiValueMap) form parameters}
 * is not supported: the Google identity is only ever bound through the
 * OIDC redirect flow driven by the framework's OAuth2 login success
 * handler, which supplies a pre-verified prototype.
 */
@Service
public class GoogleUserIdentityService extends AbstractUserIdentityService {

    /** Public {@code identity_type} value for the Google backend. */
    public static final String IDENTITY_TYPE = "google";

    /**
     * Extension key carrying the raw Google {@code sub} on a prototype
     * {@link UserIdentity}. When absent the backend falls back to the
     * standard OIDC {@code sub} claim key.
     */
    public static final String EXTENSION_SUB = "sub";
    public static final String EXTENSION_EMAIL = "email";
    public static final String EXTENSION_EMAIL_VERIFIED = "emailVerified";
    public static final String EXTENSION_NAME = "name";
    public static final String EXTENSION_GIVEN_NAME = "givenName";
    public static final String EXTENSION_FAMILY_NAME = "familyName";
    public static final String EXTENSION_PICTURE = "picture";
    public static final String EXTENSION_LOCALE = "locale";

    private final UserIdentityRepository identityRepository;
    private final UserIdentityGoogleRepository identityGoogleRepository;

    public GoogleUserIdentityService(UserIdentityRepository identityRepository,
                                     UserIdentityGoogleRepository identityGoogleRepository) {
        Assert.notNull(identityRepository, "identityRepository is required");
        Assert.notNull(identityGoogleRepository, "identityGoogleRepository is required");
        this.identityRepository = identityRepository;
        this.identityGoogleRepository = identityGoogleRepository;
    }

    @Override
    public String identityType() {
        return IDENTITY_TYPE;
    }

    @Override
    public UserIdentity createUserIdentity(String userId, MultiValueMap<String, String> params) {
        throw new InvalidUserIdentityException(
                "Google identity is bound through OIDC login only; cannot be created via form parameters");
    }

    @Override
    @Transactional
    public UserIdentity createUserIdentity(String userId, UserIdentity prototype) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(prototype, "prototype must not be null");
        if (!IDENTITY_TYPE.equals(prototype.getIdentityType())) {
            throw new InvalidUserIdentityException(
                    "identityType '" + prototype.getIdentityType()
                            + "' is not supported by the google backend");
        }
        String sub = prototype.getProperty(EXTENSION_SUB);
        if (!StringUtils.hasText(sub)) {
            throw new InvalidUserIdentityException(
                    "prototype extension '" + EXTENSION_SUB + "' is required");
        }
        if (this.identityRepository.existsByIdentityTypeAndSubject(IDENTITY_TYPE, sub)) {
            throw new IdentityOccupiedException(IDENTITY_TYPE);
        }
        UserIdentityEntity identity = new UserIdentityEntity();
        identity.setUserId(userId);
        identity.setIdentityType(IDENTITY_TYPE);
        identity.setSubject(sub);
        identity.setBoundAt(Instant.now());
        identity = this.identityRepository.save(identity);

        UserIdentityGoogleEntity child = new UserIdentityGoogleEntity();
        child.setIdentityId(identity.getId());
        populateChild(child, prototype);
        this.identityGoogleRepository.save(child);

        return toModel(identity, child);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserIdentity> getUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        return this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE)
                .map(identity -> toModel(identity, loadChild(identity.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserIdentity> listUserIdentities(String userId) {
        Assert.hasText(userId, "userId must not be empty");
        List<UserIdentityEntity> identities = this.identityRepository
                .findAllByUserIdAndIdentityType(userId, IDENTITY_TYPE);
        if (identities.isEmpty()) {
            return List.of();
        }
        Map<String, UserIdentityGoogleEntity> childrenByIdentityId =
                new HashMap<>(identities.size());
        for (UserIdentityEntity identity : identities) {
            UserIdentityGoogleEntity child = loadChild(identity.getId());
            if (child != null) {
                childrenByIdentityId.put(identity.getId(), child);
            }
        }
        return identities.stream()
                .map(identity -> toModel(identity,
                        childrenByIdentityId.get(identity.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserIdentity> listUserIdentities(String userId, String identityType) {
        Assert.isTrue(IDENTITY_TYPE.equals(identityType),
                "Unsupported identity type: " + identityType);
        return this.listUserIdentities(userId);
    }

    @Override
    public UserIdentity updateUserIdentity(String userId, String identityId,
                                           MultiValueMap<String, String> params) {
        throw new InvalidUserIdentityException(
                "Google identity cannot be updated via form parameters");
    }

    @Override
    @Transactional
    public UserIdentity updateUserIdentity(String userId, String identityId, UserIdentity prototype) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Assert.notNull(prototype, "prototype must not be null");

        UserIdentityEntity identity = this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE)
                .orElseThrow(() -> new UserIdentityNotFoundException(identityId));

        String newSub = prototype.getProperty(EXTENSION_SUB);
        if (!StringUtils.hasText(newSub)) {
            throw new InvalidUserIdentityException(
                    "prototype extension '" + EXTENSION_SUB + "' is required");
        }
        boolean changingSubject = !newSub.equals(identity.getSubject());
        if (changingSubject
                && this.identityRepository.existsByIdentityTypeAndSubject(IDENTITY_TYPE, newSub)) {
            throw new IdentityOccupiedException(IDENTITY_TYPE);
        }
        identity.setSubject(newSub);
        this.identityRepository.save(identity);

        UserIdentityGoogleEntity child =
                this.identityGoogleRepository.findById(identityId).orElseGet(() -> {
                    UserIdentityGoogleEntity fresh = new UserIdentityGoogleEntity();
                    fresh.setIdentityId(identityId);
                    return fresh;
                });
        populateChild(child, prototype);
        this.identityGoogleRepository.save(child);

        return toModel(identity, child);
    }

    @Override
    @Transactional
    public void deleteUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Optional<UserIdentityEntity> identity = this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE);
        if (identity.isEmpty()) {
            // Not a google identity, or not owned by this user; per SPI
            // contract return silently so the wire layer cannot probe.
            return;
        }
        this.identityGoogleRepository.deleteById(identityId);
        this.identityRepository.delete(identity.get());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserIdentity> findUserIdentityByRawSubject(String identityType, String rawSubject) {
        if (!IDENTITY_TYPE.equals(identityType) || !StringUtils.hasText(rawSubject)) {
            return Optional.empty();
        }
        // Google `sub` is the identity function's own value; no hashing.
        return this.identityRepository.findByIdentityTypeAndSubject(IDENTITY_TYPE, rawSubject)
                .map(identity -> toModel(identity, loadChild(identity.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRawFieldValue(String userId, String identityId, String fieldName) {
        if (!EXTENSION_EMAIL.equals(fieldName)
                && !EXTENSION_EMAIL_VERIFIED.equals(fieldName)
                && !EXTENSION_NAME.equals(fieldName)
                && !EXTENSION_GIVEN_NAME.equals(fieldName)
                && !EXTENSION_FAMILY_NAME.equals(fieldName)
                && !EXTENSION_PICTURE.equals(fieldName)
                && !EXTENSION_LOCALE.equals(fieldName)) {
            return Optional.empty();
        }
        return this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE)
                .flatMap(identity -> this.identityGoogleRepository.findById(identity.getId()))
                .map(child -> switch (fieldName) {
                    case EXTENSION_EMAIL -> child.getEmail();
                    case EXTENSION_EMAIL_VERIFIED -> child.getEmailVerified() != null
                            ? child.getEmailVerified().toString() : null;
                    case EXTENSION_NAME -> child.getName();
                    case EXTENSION_GIVEN_NAME -> child.getGivenName();
                    case EXTENSION_FAMILY_NAME -> child.getFamilyName();
                    case EXTENSION_PICTURE -> child.getPicture();
                    case EXTENSION_LOCALE -> child.getLocale();
                    default -> null;
                });
    }

    // ---- helpers ----

    private UserIdentityGoogleEntity loadChild(String identityId) {
        return this.identityGoogleRepository.findById(identityId).orElse(null);
    }

    /**
     * Copy profile attributes from a prototype into the child entity.
     * Values in the prototype's extensions map carry their original types
     * from the upstream IdP (e.g. {@code Boolean} for {@code email_verified},
     * {@code String} for everything else); type coercion happens here at
     * the persistence boundary.
     */
    private void populateChild(UserIdentityGoogleEntity child, UserIdentity prototype) {
        Map<String, Object> ext = prototype.getExtensions();
        child.setEmail(MapUtils.getString(ext, EXTENSION_EMAIL));
        child.setEmailVerified(MapUtils.getBoolean(ext, EXTENSION_EMAIL_VERIFIED));
        child.setName(MapUtils.getString(ext, EXTENSION_NAME));
        child.setGivenName(MapUtils.getString(ext, EXTENSION_GIVEN_NAME));
        child.setFamilyName(MapUtils.getString(ext, EXTENSION_FAMILY_NAME));
        child.setPicture(MapUtils.getString(ext, EXTENSION_PICTURE));
        child.setLocale(MapUtils.getString(ext, EXTENSION_LOCALE));
    }

    private UserIdentity toModel(UserIdentityEntity identity, UserIdentityGoogleEntity child) {
        Map<String, Object> extensions = new LinkedHashMap<>();
        if (child != null) {
            extensions.put(EXTENSION_EMAIL, child.getEmail());
            extensions.put(EXTENSION_EMAIL_VERIFIED, child.getEmailVerified());
            extensions.put(EXTENSION_NAME, child.getName());
            extensions.put(EXTENSION_GIVEN_NAME, child.getGivenName());
            extensions.put(EXTENSION_FAMILY_NAME, child.getFamilyName());
            extensions.put(EXTENSION_PICTURE, child.getPicture());
            extensions.put(EXTENSION_LOCALE, child.getLocale());
        }
        return UserIdentity.withExtensions(extensions)
                .identityId(identity.getId())
                .identityType(identity.getIdentityType())
                .subject(identity.getSubject())
                .userId(identity.getUserId())
                .boundAt(identity.getBoundAt())
                .build();
    }
}
