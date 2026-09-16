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
package org.eulerframework.uc.oauth2.service;

import org.eulerframework.security.oauth2.core.EulerAuthorizationGrantType;
import org.eulerframework.security.oauth2.core.EulerClientAuthenticationMethod;
import org.eulerframework.uc.oauth2.entity.OAuth2ClientEntity;
import org.eulerframework.uc.oauth2.repository.OAuth2ClientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the deprecated {@code app_assertion} grant guard in {@link OAuth2ClientService}:
 * a write may not newly introduce the grant, but a client that already persists it (a historical
 * STATIC App Attest client) is grandfathered so it can still be updated.
 */
class OAuth2ClientServiceTest {

    private static final String APP_ASSERTION = EulerAuthorizationGrantType.APP_ASSERTION.getValue();
    private static final String REGISTRATION_ID = "reg-1";

    @Mock
    OAuth2ClientRepository oauth2ClientRepository;

    private AutoCloseable mocks;
    private OAuth2ClientService service;

    @BeforeEach
    void setUp() {
        this.mocks = MockitoAnnotations.openMocks(this);
        this.service = new OAuth2ClientService();
        this.service.setOauth2ClientRepository(this.oauth2ClientRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        this.mocks.close();
    }

    @Test
    void createRejectsAppAssertionGrant() {
        RegisteredClient client = client(EulerAuthorizationGrantType.APP_ASSERTION, AuthorizationGrantType.REFRESH_TOKEN);

        assertThatThrownBy(() -> this.service.createClient(client))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(APP_ASSERTION);
        verify(this.oauth2ClientRepository, never()).save(any());
    }

    @Test
    void createAllowsClientWithoutAppAssertionGrant() {
        RegisteredClient client = client(AuthorizationGrantType.REFRESH_TOKEN);

        this.service.createClient(client);

        verify(this.oauth2ClientRepository).save(any(OAuth2ClientEntity.class));
    }

    @Test
    void updateGrandfathersPersistedAppAssertionGrant() {
        when(this.oauth2ClientRepository.findById(REGISTRATION_ID))
                .thenReturn(Optional.of(persisted(APP_ASSERTION + ",refresh_token")));
        RegisteredClient client = client(EulerAuthorizationGrantType.APP_ASSERTION, AuthorizationGrantType.REFRESH_TOKEN);

        this.service.updateClient(client);

        verify(this.oauth2ClientRepository).save(any(OAuth2ClientEntity.class));
    }

    @Test
    void updateRejectsNewlyAddedAppAssertionGrant() {
        when(this.oauth2ClientRepository.findById(REGISTRATION_ID))
                .thenReturn(Optional.of(persisted("refresh_token")));
        RegisteredClient client = client(EulerAuthorizationGrantType.APP_ASSERTION, AuthorizationGrantType.REFRESH_TOKEN);

        assertThatThrownBy(() -> this.service.updateClient(client))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(APP_ASSERTION);
        verify(this.oauth2ClientRepository, never()).save(any());
    }

    private static RegisteredClient client(AuthorizationGrantType... grantTypes) {
        RegisteredClient.Builder builder = RegisteredClient.withId(REGISTRATION_ID)
                .clientId("client-1")
                .clientAuthenticationMethod(EulerClientAuthenticationMethod.ATTEST_JWT_CLIENT_AUTH);
        for (AuthorizationGrantType grantType : grantTypes) {
            builder.authorizationGrantType(grantType);
        }
        return builder.build();
    }

    private static OAuth2ClientEntity persisted(String grantTypes) {
        OAuth2ClientEntity entity = new OAuth2ClientEntity();
        entity.setId(REGISTRATION_ID);
        entity.setClientId("client-1");
        entity.setGrantTypes(grantTypes);
        return entity;
    }
}
