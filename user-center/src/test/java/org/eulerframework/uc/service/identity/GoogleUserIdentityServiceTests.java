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
import org.eulerframework.uc.entity.UserIdentityEntity;
import org.eulerframework.uc.entity.UserIdentityGoogleEntity;
import org.eulerframework.uc.repository.UserIdentityGoogleRepository;
import org.eulerframework.uc.repository.UserIdentityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleUserIdentityServiceTests {

    @Mock
    UserIdentityRepository identityRepository;

    @Mock
    UserIdentityGoogleRepository identityGoogleRepository;

    private AutoCloseable mocks;
    private GoogleUserIdentityService service;

    @BeforeEach
    void setUp() {
        this.mocks = MockitoAnnotations.openMocks(this);
        this.service = new GoogleUserIdentityService(this.identityRepository, this.identityGoogleRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        this.mocks.close();
    }

    @Test
    void identityTypeIsGoogle() {
        assertThat(this.service.identityType()).isEqualTo("google");
    }

    @Test
    void createFromFormParametersIsRejected() {
        assertThatThrownBy(() -> this.service.createUserIdentity("u-1", new LinkedMultiValueMap<>()))
                .isInstanceOf(InvalidUserIdentityException.class);
    }

    @Test
    void createFromPrototypePersistsBothRows() {
        UserIdentity prototype = UserIdentity.withExtensions(Map.of(
                        "sub", "google-sub-1",
                        "email", "alice@example.com",
                        "name", "Alice",
                        "picture", "https://example.com/a.png",
                        "locale", "en"))
                .identityType("google")
                .build();
        when(this.identityRepository.existsByIdentityTypeAndSubject("google", "google-sub-1"))
                .thenReturn(false);
        when(this.identityRepository.save(any(UserIdentityEntity.class)))
                .thenAnswer(inv -> {
                    UserIdentityEntity e = inv.getArgument(0);
                    // Simulate the framework-generated UUID that
                    // AuditingUUIDEntity.save() would produce.
                    e.setId("id-1");
                    return e;
                });

        UserIdentity persisted = this.service.createUserIdentity("u-1", prototype);

        assertThat(persisted.getIdentityId()).isEqualTo("id-1");
        assertThat(persisted.getSubject()).isEqualTo("google-sub-1");
        assertThat(persisted.getUserId()).isEqualTo("u-1");
        assertThat(persisted.getBoundAt()).isNotNull();

        ArgumentCaptor<UserIdentityEntity> parentCaptor = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(this.identityRepository).save(parentCaptor.capture());
        UserIdentityEntity parent = parentCaptor.getValue();
        assertThat(parent.getUserId()).isEqualTo("u-1");
        assertThat(parent.getIdentityType()).isEqualTo("google");
        assertThat(parent.getSubject()).isEqualTo("google-sub-1");

        ArgumentCaptor<UserIdentityGoogleEntity> childCaptor =
                ArgumentCaptor.forClass(UserIdentityGoogleEntity.class);
        verify(this.identityGoogleRepository).save(childCaptor.capture());
        UserIdentityGoogleEntity child = childCaptor.getValue();
        assertThat(child.getIdentityId()).isEqualTo("id-1");
        assertThat(child.getEmail()).isEqualTo("alice@example.com");
        assertThat(child.getName()).isEqualTo("Alice");
        assertThat(child.getPicture()).isEqualTo("https://example.com/a.png");
        assertThat(child.getLocale()).isEqualTo("en");
    }

    @Test
    void createRejectsPrototypeWithWrongIdentityType() {
        UserIdentity prototype = UserIdentity.withExtensions(Map.of("sub", "s"))
                .identityType("apple")
                .build();
        assertThatThrownBy(() -> this.service.createUserIdentity("u-1", prototype))
                .isInstanceOf(InvalidUserIdentityException.class);
    }

    @Test
    void createRejectsPrototypeMissingSub() {
        UserIdentity prototype = UserIdentity.withExtensions(Map.of("email", "x@y"))
                .identityType("google")
                .build();
        assertThatThrownBy(() -> this.service.createUserIdentity("u-1", prototype))
                .isInstanceOf(InvalidUserIdentityException.class);
    }

    @Test
    void createOccupiedSubjectThrows() {
        UserIdentity prototype = UserIdentity.withExtensions(Map.of("sub", "s"))
                .identityType("google")
                .build();
        when(this.identityRepository.existsByIdentityTypeAndSubject("google", "s"))
                .thenReturn(true);
        assertThatThrownBy(() -> this.service.createUserIdentity("u-1", prototype))
                .isInstanceOf(IdentityOccupiedException.class);
    }

    @Test
    void findByRawSubjectHitsAndProjectsExtensions() {
        UserIdentityEntity parent = new UserIdentityEntity();
        parent.setId("id-1");
        parent.setUserId("u-1");
        parent.setIdentityType("google");
        parent.setSubject("sub-1");
        parent.setBoundAt(Instant.now());
        when(this.identityRepository.findByIdentityTypeAndSubject("google", "sub-1"))
                .thenReturn(Optional.of(parent));

        UserIdentityGoogleEntity child = new UserIdentityGoogleEntity();
        child.setIdentityId("id-1");
        child.setEmail("bob@example.com");
        child.setName("Bob");
        when(this.identityGoogleRepository.findById("id-1"))
                .thenReturn(Optional.of(child));

        Optional<UserIdentity> found =
                this.service.findUserIdentityByRawSubject("google", "sub-1");

        assertThat(found).isPresent();
        assertThat(found.get().getIdentityId()).isEqualTo("id-1");
        assertThat(found.get().getSubject()).isEqualTo("sub-1");
        assertThat((String) found.get().getProperty("email")).isEqualTo("bob@example.com");
        assertThat((String) found.get().getProperty("name")).isEqualTo("Bob");
    }

    @Test
    void findByRawSubjectReturnsEmptyForOtherTypes() {
        Optional<UserIdentity> found =
                this.service.findUserIdentityByRawSubject("phone", "+12345");
        assertThat(found).isEmpty();
    }

    @Test
    void deleteCascadesToChildRow() {
        UserIdentityEntity parent = new UserIdentityEntity();
        parent.setId("id-1");
        parent.setUserId("u-1");
        parent.setIdentityType("google");
        parent.setSubject("sub-1");
        parent.setBoundAt(Instant.now());
        when(this.identityRepository.findByIdAndUserIdAndIdentityType("id-1", "u-1", "google"))
                .thenReturn(Optional.of(parent));

        this.service.deleteUserIdentity("u-1", "id-1");

        verify(this.identityGoogleRepository).deleteById("id-1");
        verify(this.identityRepository).delete(parent);
    }

    @Test
    void deleteSilentlyIgnoresNonGoogleIds() {
        when(this.identityRepository.findByIdAndUserIdAndIdentityType(eq("id-1"), eq("u-1"), eq("google")))
                .thenReturn(Optional.empty());
        this.service.deleteUserIdentity("u-1", "id-1");
        // No exception, no cascade.
        verify(this.identityGoogleRepository, org.mockito.Mockito.never()).deleteById(any());
    }
}
