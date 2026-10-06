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

import com.nimbusds.jose.jwk.JWK;
import org.eulerframework.security.authentication.appattest.AppAttestInstanceKeyRegistration;
import org.eulerframework.security.core.identity.IdentityOccupiedException;
import org.eulerframework.security.core.identity.InvalidUserIdentityException;
import org.eulerframework.security.core.identity.UserIdentity;
import org.eulerframework.security.core.identity.UserIdentityNotFoundException;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.security.util.JwkUtils;
import org.eulerframework.uc.entity.UserIdentityEntity;
import org.eulerframework.uc.repository.UserIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code app_attest_instance_key} backend of {@link UserIdentityService}: an identity proved
 * by possession of a private key whose public half the App instance registered with the
 * server.
 *
 * <p>Persists a parent {@code t_user_identity} row and nothing else. The whole of what this
 * backend stores is the key's RFC 7638 thumbprint, as {@link UserIdentity#getSubject()}, which
 * is both the cross-account unique key and the very value a jwt-bearer assertion names as its
 * {@code kid}. The key material is deliberately <b>not</b> copied here: a login reads it from
 * the App Attest instance-key registry, and does so on every login rather than only a first
 * one, so a second copy would be a second answer to the same question with nothing keeping the
 * two together. What the account has to contribute is which key is its own, and a thumbprint
 * says that completely.
 *
 * <p>Three rules set this backend apart from the others:
 * <ul>
 *   <li><b>It may not share an account with any other <em>kind</em> of identity.</b> A key
 *       proves possession of a device, not who is holding it, so an account identified this
 *       way is an anonymous trial until something better is bound to it &mdash; and from that
 *       moment this identity must stop authenticating. Refusing to bind alongside a phone,
 *       email or federated identity is what makes that immediate, with nothing to delete and
 *       no data lost; the authentication side enforces the same rule for accounts that were
 *       bound the other way round.</li>
 *   <li><b>One key per account.</b> A second key could only be added by a caller who can
 *       <em>name</em> the account, and naming it proves nothing: the name is the token
 *       {@code sub}, which every access token this server issues carries and every resource
 *       server it is presented to therefore sees. Admitting a second key on that evidence
 *       would turn a public identifier into the account's credential, so the key an account
 *       was opened with is the only one it ever gets. The cost is stated plainly rather than
 *       engineered around: lose the private half and the account is unreachable.</li>
 *   <li><b>The binding never changes.</b> There is no rotation and no update: a caller who
 *       loses the private half registers a new key and opens a new account, and the old one
 *       is left behind. What is immutable here is the subject this row carries, which is what
 *       lets a login trust that the key the issuer vouched for is the key the account was
 *       opened with.</li>
 * </ul>
 *
 * <p>Creation via
 * {@link #createUserIdentity(String, MultiValueMap) form parameters} is not supported: a
 * key is only ever bound by a jwt-bearer login that has already verified a signature made
 * by it, which supplies a pre-verified prototype. The prototype still carries the JWK, since
 * the thumbprint has to be derived from the key itself; it is read, hashed, and not kept.
 */
@Service
public class AppAttestInstanceKeyUserIdentityService extends AbstractUserIdentityService {

    private final UserIdentityRepository identityRepository;

    public AppAttestInstanceKeyUserIdentityService(UserIdentityRepository identityRepository) {
        Assert.notNull(identityRepository, "identityRepository is required");
        this.identityRepository = identityRepository;
    }

    @Override
    public String identityType() {
        return AppAttestInstanceKeyRegistration.USER_IDENTITY_TYPE;
    }

    @Override
    public UserIdentity createUserIdentity(String userId, MultiValueMap<String, String> params) {
        throw new InvalidUserIdentityException(
                "An app_attest_instance_key identity is bound by a verified jwt-bearer login only; "
                        + "it cannot be created via form parameters");
    }

    @Override
    @Transactional
    public UserIdentity createUserIdentity(String userId, UserIdentity prototype) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.notNull(prototype, "prototype must not be null");
        if (!identityType().equals(prototype.getIdentityType())) {
            throw new InvalidUserIdentityException(
                    "identityType '" + prototype.getIdentityType()
                            + "' is not supported by the app_attest_instance_key backend");
        }

        // The subject is the thumbprint: the cross-account unique key, and the same value the
        // assertion header carries as its kid, which is how a login matches the two up.
        JWK publicKey = normalize(readPrototypeJwk(prototype));
        String subject = JwkUtils.computeThumbprint(publicKey);

        // Exclusivity by kind: the moment the account is also identified by something that
        // proves who the person is, this weak factor must stop authenticating it.
        if (this.identityRepository.existsByUserIdAndIdentityTypeNot(userId, identityType())) {
            throw new IdentityOccupiedException(identityType(),
                    "The account already carries an identity of another type, "
                            + "so it cannot be authenticated by a public key");
        }
        // One key per account. Whoever asks for a second one can only have named this
        // account, and its name is the token sub - public to every resource server an access
        // token was shown to. Refusing here is what keeps that name from being a credential.
        if (this.identityRepository.existsByUserIdAndIdentityType(userId, identityType())) {
            throw new IdentityOccupiedException(identityType(),
                    "The account already carries a public key and cannot be given a second one");
        }
        if (this.identityRepository.existsByIdentityTypeAndSubject(identityType(), subject)) {
            throw new IdentityOccupiedException(identityType());
        }

        Instant now = Instant.now();
        UserIdentityEntity identity = new UserIdentityEntity();
        identity.setUserId(userId);
        identity.setIdentityType(identityType());
        identity.setSubject(subject);
        identity.setBoundAt(now);
        identity = this.identityRepository.save(identity);

        return toModel(identity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserIdentity> getUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        return this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, identityType())
                .map(this::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserIdentity> listUserIdentities(String userId) {
        Assert.hasText(userId, "userId must not be empty");
        return this.identityRepository.findAllByUserIdAndIdentityType(userId, identityType())
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserIdentity> listUserIdentities(String userId, String identityType) {
        Assert.isTrue(identityType().equals(identityType), "Unsupported identity type: " + identityType);
        return this.listUserIdentities(userId);
    }

    @Override
    public UserIdentity updateUserIdentity(String userId, String identityId,
                                           MultiValueMap<String, String> params) {
        throw new InvalidUserIdentityException(
                "An app_attest_instance_key identity cannot be updated; register a new key instead");
    }

    @Override
    public UserIdentity updateUserIdentity(String userId, String identityId, UserIdentity prototype) {
        throw new InvalidUserIdentityException(
                "An app_attest_instance_key identity cannot be updated; register a new key instead");
    }

    @Override
    @Transactional
    public void deleteUserIdentity(String userId, String identityId) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(identityId, "identityId must not be empty");
        // TODO Refuse when this is the account's last identity of any type; the check has to
        //      sit on DelegatingUserIdentityService, which is the only place that sees every
        //      type. Acute here because the key is an anonymous account's only credential:
        //      unlike a phone or email identity, nothing else can reach the account once it is
        //      gone, and the client still holds a sub that now resolves to nothing.
        Optional<UserIdentityEntity> identity = this.identityRepository
                .findByIdAndUserIdAndIdentityType(identityId, userId, identityType());
        if (identity.isEmpty()) {
            // Not an app_attest_instance_key identity, or not owned by this user; per the SPI
            // contract return silently so the wire layer cannot probe ownership.
            return;
        }
        this.identityRepository.delete(identity.get());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserIdentity> findUserIdentityByRawSubject(String identityType, String rawSubject) {
        if (!identityType().equals(identityType) || !StringUtils.hasText(rawSubject)) {
            return Optional.empty();
        }
        // The raw value is the key itself, so the transform is its thumbprint; parsing is what
        // makes the lookup independent of how the caller serialised it.
        String subject = JwkUtils.computeThumbprint(parse(rawSubject));
        return this.identityRepository.findByIdentityTypeAndSubject(identityType(), subject)
                .map(this::toModel);
    }

    @Override
    public Optional<String> getRawFieldValue(String userId, String identityId, String fieldName) {
        // Nothing here is masked: the key is public and already surfaces whole on the
        // identity projection.
        return Optional.empty();
    }

    // ---- helpers ----

    private static Object readPrototypeJwk(UserIdentity prototype) {
        Object jwk = prototype.getProperty(UserIdentityService.PROPERTY_JWK);
        if (jwk == null) {
            throw new InvalidUserIdentityException(
                    "prototype extension '" + UserIdentityService.PROPERTY_JWK + "' is required");
        }
        return jwk;
    }

    /**
     * Read the prototype's key, whatever JSON form it arrived in, and reduce it to what may
     * be persisted: public-only, and of a key type the login flow can verify a signature
     * with. Enforced at this boundary rather than trusted from the caller, because a private
     * member reaching this column would be a secret stored where it is meant to be readable.
     */
    private static JWK normalize(Object prototypeJwk) {
        JWK parsed;
        try {
            if (prototypeJwk instanceof Map<?, ?> members) {
                parsed = JWK.parse(jwkMembers(members));
            } else {
                parsed = parse(String.valueOf(prototypeJwk));
            }
            return JwkUtils.toPublicJwk(parsed);
        } catch (InvalidUserIdentityException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidUserIdentityException(
                    "prototype extension '" + UserIdentityService.PROPERTY_JWK + "' is not a registrable JWK: "
                            + e.getMessage(), e);
        }
    }

    private static JWK parse(String jwkJson) {
        if (!StringUtils.hasText(jwkJson)) {
            throw new InvalidUserIdentityException("The JWK is empty");
        }
        try {
            return JWK.parse(jwkJson);
        } catch (ParseException | RuntimeException e) {
            throw new InvalidUserIdentityException("The JWK is not parsable: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")  // a JWK's JSON object form is Map<String, Object> by construction
    private static Map<String, Object> jwkMembers(Map<?, ?> members) {
        return (Map<String, Object>) members;
    }

    /**
     * Project the identity. It carries no extension: the subject <em>is</em> the key's
     * thumbprint, and the key material lives in the App Attest instance-key registry, so there
     * is nothing this backend holds that could be handed back here. A caller wanting the key
     * asks the registry, which is where a login gets it too.
     */
    private UserIdentity toModel(UserIdentityEntity identity) {
        return UserIdentity.withExtensions(new LinkedHashMap<>())
                .identityId(identity.getId())
                .identityType(identity.getIdentityType())
                .subject(identity.getSubject())
                .userId(identity.getUserId())
                .boundAt(identity.getBoundAt())
                .build();
    }
}
