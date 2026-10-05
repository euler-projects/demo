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

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eulerframework.security.core.identity.IdentityOccupiedException;
import org.eulerframework.security.core.identity.InvalidUserIdentityException;
import org.eulerframework.security.core.identity.UserIdentity;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.security.util.JwkUtils;
import org.eulerframework.uc.entity.UserIdentityEntity;
import org.eulerframework.uc.entity.UserIdentityPublicKeyEntity;
import org.eulerframework.uc.repository.UserIdentityPublicKeyRepository;
import org.eulerframework.uc.repository.UserIdentityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PublicKeyUserIdentityService}, focused on the three rules that set this
 * backend apart: the key's thumbprint is its cross-account identity, it may not share an
 * account with another kind of identity, and an account gets exactly one of them.
 */
class PublicKeyUserIdentityServiceTests {

    private static final String USER_ID = "u-1";

    @Mock
    UserIdentityRepository identityRepository;

    @Mock
    UserIdentityPublicKeyRepository identityPublicKeyRepository;

    private AutoCloseable mocks;
    private PublicKeyUserIdentityService service;
    private ECKey key;
    private final AtomicInteger nextIdentityId = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        this.mocks = MockitoAnnotations.openMocks(this);
        this.service = new PublicKeyUserIdentityService(this.identityRepository,
                this.identityPublicKeyRepository);
        this.key = new ECKeyGenerator(Curve.P_256).generate();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.mocks.close();
    }

    @Test
    void identityTypeIsPublicKey() {
        assertThat(this.service.identityType()).isEqualTo(UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY);
    }

    @Test
    void createDerivesSubjectAndKidFromTheKeyMaterial() throws Exception {
        stubParentSave();

        UserIdentity persisted = this.service.createUserIdentity(USER_ID, prototype(this.key));

        String thumbprint = JwkUtils.computeThumbprint(this.key);
        assertThat(persisted.getSubject()).isEqualTo(thumbprint);
        assertThat(persisted.getUserId()).isEqualTo(USER_ID);
        assertThat(persisted.getIdentityId()).isEqualTo("id-1");

        ArgumentCaptor<UserIdentityPublicKeyEntity> childCaptor =
                ArgumentCaptor.forClass(UserIdentityPublicKeyEntity.class);
        verify(this.identityPublicKeyRepository).save(childCaptor.capture());
        JWK stored = JWK.parse(childCaptor.getValue().getJwk());
        // The kid is forced to the thumbprint rather than merely defaulted, so the value a
        // login selects by and the value this row is unique on cannot disagree.
        assertThat(stored.getKeyID()).isEqualTo(thumbprint);
        assertThat(stored.isPrivate()).isFalse();
    }

    @Test
    void createStripsPrivateMaterialBeforePersisting() throws Exception {
        stubParentSave();

        this.service.createUserIdentity(USER_ID,
                prototypeWithExtensions(Map.of(UserIdentityService.PROPERTY_JWK, this.key.toJSONString())));

        ArgumentCaptor<UserIdentityPublicKeyEntity> childCaptor =
                ArgumentCaptor.forClass(UserIdentityPublicKeyEntity.class);
        verify(this.identityPublicKeyRepository).save(childCaptor.capture());
        // The column is meant to be readable, so no private member may reach it however the
        // caller happened to serialise the key.
        assertThat(JWK.parse(childCaptor.getValue().getJwk()).isPrivate()).isFalse();
    }

    @Test
    void createRefusesAnAccountThatAlreadyHasAnotherIdentity() {
        when(this.identityRepository.existsByUserIdAndIdentityTypeNot(
                USER_ID, UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY)).thenReturn(true);

        assertThatThrownBy(() -> this.service.createUserIdentity(USER_ID, prototype(this.key)))
                .isInstanceOf(IdentityOccupiedException.class);

        verify(this.identityRepository, never()).save(any());
        verify(this.identityPublicKeyRepository, never()).save(any());
    }

    /**
     * One key per account. A second one could only be asked for by someone able to
     * <em>name</em> the account, and its name is the token {@code sub} &mdash; carried by
     * every access token this server issues and seen by every resource server one is shown
     * to, so not a secret. Refusing here is what keeps that name from becoming the account's
     * credential; the login path refuses the same request for the same reason.
     */
    @Test
    void createRefusesASecondKeyOnTheSameAccount() {
        when(this.identityRepository.existsByUserIdAndIdentityType(
                USER_ID, UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY)).thenReturn(true);

        assertThatThrownBy(() -> this.service.createUserIdentity(USER_ID, prototype(this.key)))
                .isInstanceOf(IdentityOccupiedException.class);

        verify(this.identityRepository, never()).save(any());
        verify(this.identityPublicKeyRepository, never()).save(any());
    }

    @Test
    void createRefusesAKeyAlreadyBoundToAnotherAccount() throws Exception {
        when(this.identityRepository.existsByIdentityTypeAndSubject(
                UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY, JwkUtils.computeThumbprint(this.key)))
                .thenReturn(true);

        assertThatThrownBy(() -> this.service.createUserIdentity(USER_ID, prototype(this.key)))
                .isInstanceOf(IdentityOccupiedException.class);

        verify(this.identityRepository, never()).save(any());
    }

    @Test
    void createFromFormParametersIsRejected() {
        assertThatThrownBy(() -> this.service.createUserIdentity(USER_ID, new LinkedMultiValueMap<>()))
                .isInstanceOf(InvalidUserIdentityException.class);
    }

    @Test
    void createRejectsPrototypeWithWrongIdentityType() {
        UserIdentity prototype = UserIdentity
                .withExtensions(Map.of(UserIdentityService.PROPERTY_JWK, "{}"))
                .identityType("phone")
                .build();

        assertThatThrownBy(() -> this.service.createUserIdentity(USER_ID, prototype))
                .isInstanceOf(InvalidUserIdentityException.class);
    }

    @Test
    void createRejectsPrototypeWithoutAKey() {
        UserIdentity prototype = UserIdentity.withExtensions(Map.of())
                .identityType(UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY)
                .build();

        assertThatThrownBy(() -> this.service.createUserIdentity(USER_ID, prototype))
                .isInstanceOf(InvalidUserIdentityException.class);
    }

    @Test
    void updateIsRejectedBecauseAKeyIsNeverRotated() {
        assertThatThrownBy(() -> this.service.updateUserIdentity(USER_ID, "id-1", new LinkedMultiValueMap<>()))
                .isInstanceOf(InvalidUserIdentityException.class);
        assertThatThrownBy(() -> this.service.updateUserIdentity(USER_ID, "id-1", prototype(this.key)))
                .isInstanceOf(InvalidUserIdentityException.class);
    }

    /**
     * The reverse lookup takes the key itself, so how a caller serialised it cannot matter:
     * two JSON forms of one key have to find the same identity.
     */
    @Test
    void findByRawSubjectIgnoresHowTheKeyWasSerialised() throws Exception {
        String thumbprint = JwkUtils.computeThumbprint(this.key);
        when(this.identityRepository.findByIdentityTypeAndSubject(
                UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY, thumbprint))
                .thenReturn(Optional.of(parent(thumbprint)));

        Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put("kid", "ignored-by-the-thumbprint");
        reordered.putAll(JwkUtils.toPublicJwk(this.key).toJSONObject());

        assertThat(this.service.findUserIdentityByRawSubject(
                UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY, this.key.toJSONString()))
                .isPresent();
        assertThat(this.service.findUserIdentityByRawSubject(
                UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY, JWK.parse(reordered).toJSONString()))
                .isPresent();
    }

    @Test
    void findByRawSubjectIgnoresAnotherIdentityType() {
        assertThat(this.service.findUserIdentityByRawSubject("phone", "anything")).isEmpty();
    }

    /** The projection hands the key back as a JSON object, not as an escaped string. */
    @Test
    void projectsTheKeyAsAJsonObject() throws Exception {
        String thumbprint = JwkUtils.computeThumbprint(this.key);
        when(this.identityRepository.findByIdAndUserIdAndIdentityType("id-1", USER_ID,
                UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY))
                .thenReturn(Optional.of(parent(thumbprint)));
        when(this.identityPublicKeyRepository.findById("id-1"))
                .thenReturn(Optional.of(child(thumbprint, JwkUtils.withKeyId(
                        JwkUtils.toPublicJwk(this.key), thumbprint).toJSONString())));

        UserIdentity read = this.service.getUserIdentity(USER_ID, "id-1").orElseThrow();

        Object projected = read.getExtensions().get(UserIdentityService.PROPERTY_JWK);
        assertThat(projected).isInstanceOf(Map.class);
        Map<?, ?> projectedKey = (Map<?, ?>) projected;
        assertThat(projectedKey.get("kty")).isEqualTo("EC");
        assertThat(projectedKey.get("kid")).isEqualTo(thumbprint);
    }

    @Test
    void nothingIsMaskedSoThereIsNoRawFieldToReveal() {
        assertThat(this.service.getRawFieldValue(USER_ID, "id-1", UserIdentityService.PROPERTY_JWK)).isEmpty();
    }

    // ---- helpers ----

    private void stubParentSave() {
        when(this.identityRepository.save(any(UserIdentityEntity.class)))
                .thenAnswer(inv -> {
                    UserIdentityEntity entity = inv.getArgument(0);
                    // Simulate the framework-generated UUID that AuditingUUIDEntity.save() produces.
                    entity.setId("id-" + this.nextIdentityId.incrementAndGet());
                    return entity;
                });
    }

    private static UserIdentity prototype(ECKey key) throws Exception {
        return prototypeWithExtensions(
                Map.of(UserIdentityService.PROPERTY_JWK, JwkUtils.toPublicJwk(key).toJSONString()));
    }

    private static UserIdentity prototypeWithExtensions(Map<String, Object> extensions) {
        return UserIdentity.withExtensions(extensions)
                .identityType(UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY)
                .build();
    }

    private static UserIdentityEntity parent(String subject) {
        UserIdentityEntity entity = new UserIdentityEntity();
        entity.setId("id-1");
        entity.setUserId(USER_ID);
        entity.setIdentityType(UserIdentityService.IDENTITY_TYPE_PUBLIC_KEY);
        entity.setSubject(subject);
        entity.setBoundAt(Instant.now());
        return entity;
    }

    private static UserIdentityPublicKeyEntity child(String identityId, String jwk) {
        UserIdentityPublicKeyEntity entity = new UserIdentityPublicKeyEntity();
        entity.setIdentityId(identityId);
        entity.setJwk(jwk);
        return entity;
    }
}
