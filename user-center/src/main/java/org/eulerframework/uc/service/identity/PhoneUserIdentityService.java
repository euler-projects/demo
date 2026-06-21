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

import org.eulerframework.security.core.identity.IdentityOccupiedException;
import org.eulerframework.security.core.identity.InvalidUserIdentityException;
import org.eulerframework.security.core.identity.UserIdentity;
import org.eulerframework.security.core.identity.UserIdentityNotFoundException;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.security.authentication.otp.OtpTicketService;
import org.eulerframework.security.authentication.otp.OtpVerification;
import org.eulerframework.uc.entity.UserIdentityEntity;
import org.eulerframework.uc.entity.UserIdentityPhoneEntity;
import org.eulerframework.uc.repository.UserIdentityPhoneRepository;
import org.eulerframework.uc.repository.UserIdentityRepository;
import org.eulerframework.uc.util.PhoneIdentifierHasher;
import org.eulerframework.uc.util.PhoneMasker;
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
 * {@code phone} backend of {@link UserIdentityService}.
 *
 * <p>Persists a parent {@code t_user_identity} row plus a child
 * {@code t_user_identity_phone} row holding the AES-256-GCM-encrypted
 * E.164 original. The cross-account unique key &mdash; the SHA-256 hex
 * of the phone &mdash; is stored as {@link UserIdentity#subject()} on
 * the parent row.
 *
 * <p>The OTP credential is not persisted on this aggregate: one-time
 * password values are managed by {@link OtpTicketService} in a separate
 * ticket cache.
 */
@Service
public class PhoneUserIdentityService extends AbstractUserIdentityService {

    /**
     * Public {@code identity_type} value for the phone backend; matches
     * the wire string surfaced on {@code /user/identities}.
     */
    public static final String IDENTITY_TYPE = "phone";

    /**
     * Extension key for the E.164 phone:
     * <ul>
     *   <li>in a prototype {@code UserIdentity}, the raw E.164 number
     *       whose SHA-256 hex becomes the persisted {@code subject};</li>
     *   <li>in a persisted {@code UserIdentity}, the masked phone
     *       string surfaced to the client.</li>
     * </ul>
     */
    private static final String EXTENSION_PHONE = "phone";

    private static final String PARAM_OTP_TICKET = "otp_ticket";
    private static final String PARAM_OTP = "otp";

    private final OtpTicketService otpTicketService;
    private final UserIdentityRepository identityRepository;
    private final UserIdentityPhoneRepository identityPhoneRepository;

    public PhoneUserIdentityService(OtpTicketService otpTicketService,
                                    UserIdentityRepository identityRepository,
                                    UserIdentityPhoneRepository identityPhoneRepository) {
        Assert.notNull(otpTicketService, "otpTicketService is required");
        Assert.notNull(identityRepository, "identityRepository is required");
        Assert.notNull(identityPhoneRepository, "identityPhoneRepository is required");
        this.otpTicketService = otpTicketService;
        this.identityRepository = identityRepository;
        this.identityPhoneRepository = identityPhoneRepository;
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
        String phone = consumeOtp(params);
        return doCreate(userId, phone);
    }

    @Override
    @Transactional
    public UserIdentity createUserIdentity(String userId, UserIdentity prototype) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(prototype, "prototype must not be null");
        if (!IDENTITY_TYPE.equals(prototype.identityType())) {
            throw new InvalidUserIdentityException(
                    "identityType '" + prototype.identityType()
                            + "' is not supported by the phone backend");
        }
        Object rawPhone = prototype.extensions().get(EXTENSION_PHONE);
        if (!(rawPhone instanceof String s) || !StringUtils.hasText(s)) {
            throw new InvalidUserIdentityException(
                    "prototype.extensions['" + EXTENSION_PHONE + "'] is required");
        }
        return doCreate(userId, s);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserIdentity> getUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        return this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE)
                .map(identity -> toModel(identity, loadPhone(identity.getId())));
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
        Map<String, String> phoneByIdentityId = new HashMap<>(identities.size());
        for (UserIdentityEntity identity : identities) {
            String phone = loadPhone(identity.getId());
            if (phone != null) {
                phoneByIdentityId.put(identity.getId(), phone);
            }
        }
        return identities.stream()
                .map(identity -> toModel(identity, phoneByIdentityId.get(identity.getId())))
                .toList();
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

        String phone = consumeOtp(params);
        String newSubject = PhoneIdentifierHasher.hash(phone);

        // Same-phone rebind is a no-op on subject; a different account
        // already owning the new value is a conflict.
        boolean changingSubject = !newSubject.equals(identity.getSubject());
        if (changingSubject
                && this.identityRepository.existsByIdentityTypeAndSubject(IDENTITY_TYPE, newSubject)) {
            throw new IdentityOccupiedException(IDENTITY_TYPE);
        }

        identity.setSubject(newSubject);
        this.identityRepository.save(identity);

        UserIdentityPhoneEntity child = this.identityPhoneRepository.findById(identityId).orElse(null);
        if (child == null) {
            child = new UserIdentityPhoneEntity();
            child.setIdentityId(identityId);
        }
        child.setPhone(phone);
        this.identityPhoneRepository.save(child);

        return toModel(identity, phone);
    }

    @Override
    @Transactional
    public void deleteUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        Optional<UserIdentityEntity> identity = this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, IDENTITY_TYPE);
        if (identity.isEmpty()) {
            // Not a phone identity, or not owned by this user; return
            // silently so the wire layer cannot probe ownership.
            return;
        }
        // Cascade the child row before the parent. The schema deliberately
        // omits a database-level foreign key so the two tables may live on
        // separate schemas or replicas if a deployment requires it.
        this.identityPhoneRepository.deleteById(identityId);
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
        String subject = PhoneIdentifierHasher.hash(rawSubject);
        return this.identityRepository.findByIdentityTypeAndSubject(IDENTITY_TYPE, subject)
                .map(identity -> toModel(identity, rawSubject));
    }

    // ---- helpers ----

    /**
     * Shared write path for both {@code createUserIdentity} overloads.
     * Derives the persisted subject from the verified raw phone,
     * enforces cross-account uniqueness, persists the parent and child
     * rows, and returns the persisted model.
     */
    private UserIdentity doCreate(String userId, String rawPhone) {
        String subject = PhoneIdentifierHasher.hash(rawPhone);
        if (this.identityRepository.existsByIdentityTypeAndSubject(IDENTITY_TYPE, subject)) {
            throw new IdentityOccupiedException(IDENTITY_TYPE);
        }
        Instant now = Instant.now();
        UserIdentityEntity identity = persistIdentity(userId, subject, now);
        persistIdentityPhone(identity.getId(), rawPhone);
        return toModel(identity, rawPhone);
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
        String phone = verification.recipient();
        if (!StringUtils.hasText(phone)) {
            throw new InvalidUserIdentityException("otp_ticket carries no recipient");
        }
        return phone;
    }

    private UserIdentityEntity persistIdentity(String userId, String subject, Instant boundAt) {
        UserIdentityEntity entity = new UserIdentityEntity();
        entity.setUserId(userId);
        entity.setIdentityType(IDENTITY_TYPE);
        entity.setSubject(subject);
        entity.setBoundAt(boundAt);
        // Hibernate populates the UUID primary key during persist.
        return this.identityRepository.save(entity);
    }

    private void persistIdentityPhone(String identityId, String phone) {
        UserIdentityPhoneEntity child = new UserIdentityPhoneEntity();
        child.setIdentityId(identityId);
        child.setPhone(phone);
        this.identityPhoneRepository.save(child);
    }

    private String loadPhone(String identityId) {
        return this.identityPhoneRepository.findById(identityId)
                .map(UserIdentityPhoneEntity::getPhone)
                .orElse(null);
    }

    private UserIdentity toModel(UserIdentityEntity identity, String phone) {
        Map<String, Object> extensions = new LinkedHashMap<>(1);
        if (StringUtils.hasText(phone)) {
            extensions.put(EXTENSION_PHONE, PhoneMasker.mask(phone));
        }
        return new UserIdentity(
                identity.getId(),
                identity.getIdentityType(),
                identity.getSubject(),
                identity.getUserId(),
                identity.getBoundAt(),
                extensions);
    }
}
