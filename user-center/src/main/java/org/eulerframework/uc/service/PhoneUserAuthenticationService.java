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

import org.eulerframework.security.authentication.factor.IdentifierConflictException;
import org.eulerframework.security.authentication.factor.InvalidAuthenticationFactorRequestException;
import org.eulerframework.security.authentication.factor.UserAuthenticationFactor;
import org.eulerframework.security.authentication.factor.UserAuthenticationFactorIdGenerator;
import org.eulerframework.security.authentication.factor.UserAuthenticationService;
import org.eulerframework.security.authentication.otp.OtpTicketService;
import org.eulerframework.security.authentication.otp.OtpVerification;
import org.eulerframework.uc.entity.UserAuthenticationFactorEntity;
import org.eulerframework.uc.entity.UserAuthenticationFactorPhoneEntity;
import org.eulerframework.uc.repository.UserAuthenticationFactorPhoneRepository;
import org.eulerframework.uc.repository.UserAuthenticationFactorRepository;
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
 * {@code phone} factor implementation of {@link UserAuthenticationService}.
 * <p>
 * Bind flow:
 * <ol>
 *     <li>Read {@code otp_ticket} and {@code otp} from the bind parameters,
 *         reject when missing with
 *         {@link InvalidAuthenticationFactorRequestException}.</li>
 *     <li>Atomically consume the OTP ticket via
 *         {@link OtpTicketService#consume(String, String, String, String)}
 *         (no PKCE / purpose enforcement on this binding endpoint), pulling
 *         the verified phone number off
 *         {@link OtpVerification#recipient()}.</li>
 *     <li>Hash the phone via {@link PhoneIdentifierHasher} for the
 *         framework-level {@code identifier}, fail-fast on
 *         {@link IdentifierConflictException} when another account already
 *         owns it.</li>
 *     <li>Persist the parent + child rows in a single transaction.</li>
 *     <li>Return the freshly bound {@link UserAuthenticationFactor} carrying
 *         {@code {"phone": <masked>}} as its only extension.</li>
 * </ol>
 */
@Service
public class PhoneUserAuthenticationService implements UserAuthenticationService {

    /**
     * Logical factor type advertised on the SPI; matches the
     * {@code factor_type=phone} value documented for the
     * {@code POST /user/identities} contract.
     */
    public static final String FACTOR_TYPE = "phone";

    private static final String PARAM_OTP_TICKET = "otp_ticket";
    private static final String PARAM_OTP = "otp";
    private static final String EXTENSION_PHONE = "phone";

    private final OtpTicketService otpTicketService;
    private final UserAuthenticationFactorRepository factorRepository;
    private final UserAuthenticationFactorPhoneRepository phoneRepository;
    private final UserAuthenticationFactorIdGenerator idGenerator;

    public PhoneUserAuthenticationService(OtpTicketService otpTicketService,
                                          UserAuthenticationFactorRepository factorRepository,
                                          UserAuthenticationFactorPhoneRepository phoneRepository) {
        Assert.notNull(otpTicketService, "otpTicketService is required");
        Assert.notNull(factorRepository, "factorRepository is required");
        Assert.notNull(phoneRepository, "phoneRepository is required");
        this.otpTicketService = otpTicketService;
        this.factorRepository = factorRepository;
        this.phoneRepository = phoneRepository;
        this.idGenerator = new UserAuthenticationFactorIdGenerator();
    }

    @Override
    public String factorType() {
        return FACTOR_TYPE;
    }

    @Override
    @Transactional
    public UserAuthenticationFactor bind(String userId, MultiValueMap<String, String> params) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(params, "params must not be null");

        String otpTicket = params.getFirst(PARAM_OTP_TICKET);
        String otp = params.getFirst(PARAM_OTP);
        if (!StringUtils.hasText(otpTicket)) {
            throw new InvalidAuthenticationFactorRequestException("otp_ticket is required");
        }
        if (!StringUtils.hasText(otp)) {
            throw new InvalidAuthenticationFactorRequestException("otp is required");
        }

        OtpVerification verification = this.otpTicketService.consume(otpTicket, null, otp, null);
        if (verification == null) {
            throw new InvalidAuthenticationFactorRequestException("otp_ticket consumption failed");
        }
        String phone = verification.recipient();
        if (!StringUtils.hasText(phone)) {
            throw new InvalidAuthenticationFactorRequestException("otp_ticket carries no recipient");
        }

        String identifier = PhoneIdentifierHasher.hash(phone);
        if (this.factorRepository.existsByFactorTypeAndIdentifier(FACTOR_TYPE, identifier)) {
            throw new IdentifierConflictException("phone already bound");
        }

        Instant now = Instant.now();
        UserAuthenticationFactorEntity parent = new UserAuthenticationFactorEntity();
        parent.setId(this.idGenerator.generate());
        parent.setUserId(userId);
        parent.setFactorType(FACTOR_TYPE);
        parent.setIdentifier(identifier);
        parent.setBoundAt(now);
        parent.setLastVerifiedAt(now);
        this.factorRepository.save(parent);

        UserAuthenticationFactorPhoneEntity child = new UserAuthenticationFactorPhoneEntity();
        child.setFactorId(parent.getId());
        child.setPhone(phone);
        this.phoneRepository.save(child);

        return toModel(parent, child);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthenticationFactor> findById(String userId, String id) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(id, "id must not be empty");
        return this.factorRepository
                .findByIdAndUserIdAndFactorType(id, userId, FACTOR_TYPE)
                .map(parent -> toModel(parent, loadPhone(parent.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAuthenticationFactor> findAllByUserId(String userId) {
        Assert.hasText(userId, "userId must not be empty");
        List<UserAuthenticationFactorEntity> parents = this.factorRepository
                .findAllByUserIdAndFactorType(userId, FACTOR_TYPE);
        if (parents.isEmpty()) {
            return List.of();
        }
        // Pre-load the children in one go to avoid an N+1 query pattern when
        // a user owns multiple phone factors.
        Map<String, UserAuthenticationFactorPhoneEntity> children = new HashMap<>(parents.size());
        for (UserAuthenticationFactorEntity parent : parents) {
            this.phoneRepository.findById(parent.getId()).ifPresent(child -> children.put(parent.getId(), child));
        }
        return parents.stream()
                .map(parent -> toModel(parent, children.get(parent.getId())))
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(String userId, String id) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(id, "id must not be empty");
        Optional<UserAuthenticationFactorEntity> parent = this.factorRepository
                .findByIdAndUserIdAndFactorType(id, userId, FACTOR_TYPE);
        if (parent.isEmpty()) {
            // Either it's not a phone factor or it doesn't belong to this
            // user — silently ignore so DelegatingUserAuthenticationService
            // can fan out without leaking ownership.
            return;
        }
        this.phoneRepository.deleteById(parent.get().getId());
        this.factorRepository.delete(parent.get());
    }

    private UserAuthenticationFactorPhoneEntity loadPhone(String factorId) {
        return this.phoneRepository.findById(factorId).orElse(null);
    }

    private UserAuthenticationFactor toModel(UserAuthenticationFactorEntity parent,
                                             UserAuthenticationFactorPhoneEntity child) {
        Map<String, Object> extensions = new LinkedHashMap<>(1);
        if (child != null && StringUtils.hasText(child.getPhone())) {
            extensions.put(EXTENSION_PHONE, PhoneMasker.mask(child.getPhone()));
        }
        return new UserAuthenticationFactor(
                parent.getId(),
                parent.getFactorType(),
                parent.getIdentifier(),
                parent.getBoundAt(),
                parent.getLastVerifiedAt(),
                extensions);
    }
}
