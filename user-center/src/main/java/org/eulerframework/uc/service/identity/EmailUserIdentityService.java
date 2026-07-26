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
import org.eulerframework.security.authentication.otp.OtpTicketService;
import org.eulerframework.security.authentication.otp.OtpVerification;
import org.eulerframework.uc.entity.UserIdentityEmailEntity;
import org.eulerframework.uc.entity.UserIdentityEntity;
import org.eulerframework.uc.repository.UserIdentityEmailRepository;
import org.eulerframework.uc.repository.UserIdentityRepository;
import org.eulerframework.uc.util.EmailIdentifierHasher;
import org.eulerframework.uc.util.EmailMasker;
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
 * {@code email} backend of {@link UserIdentityService}.
 *
 * <p>Persists a parent {@code t_user_identity} row plus a child
 * {@code t_user_identity_email} row holding the encrypted original
 * address. The cross-account unique key &mdash; the SHA-256 hex of the
 * normalised (trimmed, lower-cased) email &mdash; is stored as
 * {@link UserIdentity#getSubject()} on the parent row.
 *
 * <p>The OTP credential is not persisted on this aggregate; one-time
 * password values are managed by {@link OtpTicketService} in a separate
 * ticket cache.
 */
@Service
public class EmailUserIdentityService extends AbstractUserIdentityService {

    /**
     * Public {@code identity_type} value for the email backend; matches
     * the wire string surfaced on {@code /user/identities}.
     */
    public static final String IDENTITY_TYPE = "email";

    /**
     * Extension key for the email address:
     * <ul>
     *   <li>in a prototype {@code UserIdentity}, the raw address whose
     *       normalised SHA-256 hex becomes the persisted
     *       {@code subject};</li>
     *   <li>in a persisted {@code UserIdentity}, the masked address
     *       surfaced to the client.</li>
     * </ul>
     */
    private static final String EXTENSION_EMAIL = "email";

    private static final String PARAM_OTP_TICKET = "otp_ticket";
    private static final String PARAM_OTP = "otp";

    private final OtpTicketService otpTicketService;
    private final UserIdentityRepository identityRepository;
    private final UserIdentityEmailRepository identityEmailRepository;

    public EmailUserIdentityService(OtpTicketService otpTicketService,
                                    UserIdentityRepository identityRepository,
                                    UserIdentityEmailRepository identityEmailRepository) {
        Assert.notNull(otpTicketService, "otpTicketService is required");
        Assert.notNull(identityRepository, "identityRepository is required");
        Assert.notNull(identityEmailRepository, "identityEmailRepository is required");
        this.otpTicketService = otpTicketService;
        this.identityRepository = identityRepository;
        this.identityEmailRepository = identityEmailRepository;
    }

    @Override
    public String identityType() {
        return IDENTITY_TYPE;
    }

    @Override
    @Transactional
    public UserIdentity createUserIdentity(String userId, MultiValueMap<String, String> params) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(params, "params must not be null");
        String email = consumeOtp(params);
        return doCreate(userId, email);
    }

    @Override
    @Transactional
    public UserIdentity createUserIdentity(String userId, UserIdentity prototype) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(prototype, "prototype must not be null");
        if (!IDENTITY_TYPE.equals(prototype.getIdentityType())) {
            throw new InvalidUserIdentityException(
                    "identityType '" + prototype.getIdentityType()
                            + "' is not supported by the email backend");
        }
        String rawEmail = prototype.getProperty(EXTENSION_EMAIL);
        if (!StringUtils.hasText(rawEmail)) {
            throw new InvalidUserIdentityException(
                    "prototype extension '" + EXTENSION_EMAIL + "' is required");
        }
        return doCreate(userId, rawEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserIdentity> getUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        return this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE)
                .map(identity -> toModel(identity, loadEmail(identity.getId())));
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
        Map<String, String> emailByIdentityId = new HashMap<>(identities.size());
        for (UserIdentityEntity identity : identities) {
            String email = loadEmail(identity.getId());
            if (email != null) {
                emailByIdentityId.put(identity.getId(), email);
            }
        }
        return identities.stream()
                .map(identity -> toModel(identity, emailByIdentityId.get(identity.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserIdentity> listUserIdentities(String userId, String identityType) {
        Assert.isTrue(IDENTITY_TYPE.equals(identityType), "Unsupported identity type: " + identityType);
        return this.listUserIdentities(userId);
    }

    @Override
    @Transactional
    public UserIdentity updateUserIdentity(String userId, String identityId,
                                           MultiValueMap<String, String> params) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Assert.notNull(params, "params must not be null");

        UserIdentityEntity identity = this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE)
                .orElseThrow(() -> new UserIdentityNotFoundException(identityId));

        String email = consumeOtp(params);
        return doUpdate(identity, email);
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

        String rawEmail = prototype.getProperty(EXTENSION_EMAIL);
        if (!StringUtils.hasText(rawEmail)) {
            throw new InvalidUserIdentityException(
                    "prototype extension '" + EXTENSION_EMAIL + "' is required");
        }
        return doUpdate(identity, rawEmail);
    }

    @Override
    @Transactional
    public void deleteUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Optional<UserIdentityEntity> identity = this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE);
        if (identity.isEmpty()) {
            // Not an email identity, or not owned by this user; return
            // silently so the wire layer cannot probe ownership.
            return;
        }
        // Cascade the child row before the parent
        this.identityEmailRepository.deleteById(identityId);
        this.identityRepository.delete(identity.get());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserIdentity> findUserIdentityByRawSubject(String identityType, String rawSubject) {
        if (!IDENTITY_TYPE.equals(identityType)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(rawSubject)) {
            return Optional.empty();
        }
        String subject = EmailIdentifierHasher.hash(rawSubject);
        return this.identityRepository.findByIdentityTypeAndSubject(IDENTITY_TYPE, subject)
                .map(identity -> toModel(identity, rawSubject));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRawFieldValue(String userId, String identityId, String fieldName) {
        if (!EXTENSION_EMAIL.equals(fieldName)) {
            return Optional.empty();
        }
        return this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE)
                .flatMap(identity -> this.identityEmailRepository.findById(identity.getId()))
                .map(UserIdentityEmailEntity::getEmail);
    }

    // ---- helpers ----

    /**
     * Shared update path for both {@code updateUserIdentity} overloads.
     * Derives the new subject from the verified raw email, enforces
     * cross-account uniqueness, persists the parent and child rows, and
     * returns the persisted model.
     */
    private UserIdentity doUpdate(UserIdentityEntity identity, String rawEmail) {
        String newSubject = EmailIdentifierHasher.hash(rawEmail);

        // Same-email rebind leaves subject unchanged; rebinding to a
        // value already owned by another account is a conflict.
        boolean changingSubject = !newSubject.equals(identity.getSubject());
        if (changingSubject
                && this.identityRepository.existsByIdentityTypeAndSubject(IDENTITY_TYPE, newSubject)) {
            throw new IdentityOccupiedException(IDENTITY_TYPE);
        }

        identity.setSubject(newSubject);
        this.identityRepository.save(identity);

        String identityId = identity.getId();
        UserIdentityEmailEntity child = this.identityEmailRepository.findById(identityId).orElse(null);
        if (child == null) {
            child = new UserIdentityEmailEntity();
            child.setIdentityId(identityId);
        }
        child.setEmail(rawEmail);
        this.identityEmailRepository.save(child);

        return toModel(identity, rawEmail);
    }

    /**
     * Shared write path for both {@code createUserIdentity} overloads.
     * Derives the persisted subject from the verified raw email,
     * enforces cross-account uniqueness, persists the parent and child
     * rows, and returns the persisted model.
     */
    private UserIdentity doCreate(String userId, String rawEmail) {
        String subject = EmailIdentifierHasher.hash(rawEmail);
        if (this.identityRepository.existsByIdentityTypeAndSubject(IDENTITY_TYPE, subject)) {
            throw new IdentityOccupiedException(IDENTITY_TYPE);
        }
        Instant now = Instant.now();
        UserIdentityEntity identity = persistIdentity(userId, subject, now);
        persistIdentityEmail(identity.getId(), rawEmail);
        return toModel(identity, rawEmail);
    }

    private String consumeOtp(MultiValueMap<String, String> params) {
        String otpTicket = params.getFirst(PARAM_OTP_TICKET);
        String otp = params.getFirst(PARAM_OTP);
        if (!StringUtils.hasText(otpTicket)) {
            throw new InvalidUserIdentityException("otp_ticket is required");
        }
        if (!StringUtils.hasText(otp)) {
            throw new InvalidUserIdentityException("otp is required");
        }
        OtpVerification verification = this.otpTicketService.consume(otpTicket, null, otp, null);
        if (verification == null) {
            throw new InvalidUserIdentityException("otp_ticket consumption failed");
        }
        String email = verification.recipient();
        if (!StringUtils.hasText(email)) {
            throw new InvalidUserIdentityException("otp_ticket carries no recipient");
        }
        return email;
    }

    private UserIdentityEntity persistIdentity(String userId, String subject, Instant boundAt) {
        UserIdentityEntity entity = new UserIdentityEntity();
        entity.setUserId(userId);
        entity.setIdentityType(IDENTITY_TYPE);
        entity.setSubject(subject);
        entity.setBoundAt(boundAt);
        // Primary key is generated by Hibernate during persist.
        return this.identityRepository.save(entity);
    }

    private void persistIdentityEmail(String identityId, String email) {
        UserIdentityEmailEntity child = new UserIdentityEmailEntity();
        child.setIdentityId(identityId);
        child.setEmail(email);
        this.identityEmailRepository.save(child);
    }

    private String loadEmail(String identityId) {
        return this.identityEmailRepository.findById(identityId)
                .map(UserIdentityEmailEntity::getEmail)
                .orElse(null);
    }

    private UserIdentity toModel(UserIdentityEntity identity, String email) {
        Map<String, Object> extensions = new LinkedHashMap<>(1);
        if (StringUtils.hasText(email)) {
            extensions.put(EXTENSION_EMAIL, EmailMasker.mask(email));
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
