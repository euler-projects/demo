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
package org.eulerframework.uc.appattest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eulerframework.data.entity.AuditingEntity;
import org.eulerframework.security.authentication.appattest.RegisteredApp;
import org.springframework.data.domain.Persistable;

/**
 * JPA entity for a registered App Attest app.
 *
 * <p>The primary key {@code id} is the business {@code registrationId}. The
 * {@code appId} column persists the original {@code teamId.bundleId} value
 * verbatim and is unique, so {@code findByAppId} is a direct index lookup. The
 * {@code appIdHash} column persists the hex-encoded SHA-256 of {@code appId}
 * and is unique, so the App Attest RP ID hash lookup path (protocol side) does
 * not collide with the {@code appId} lookup path.
 */
@Entity
@Table(name = "t_app_attest_app")
public class AppEntity extends AuditingEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "app_id", length = 288, nullable = false, unique = true)
    private String appId;

    @Column(name = "app_id_hash", length = 64, nullable = false, unique = true)
    private String appIdHash;

    @Column(name = "team_id", length = 32, nullable = false)
    private String teamId;

    @Column(name = "bundle_id", length = 255, nullable = false)
    private String bundleId;

    @Column(name = "oauth2_enabled", nullable = false)
    private boolean oauth2Enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth2_client_type", length = 16)
    private RegisteredApp.OAuth2ClientType oauth2ClientType;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppIdHash() {
        return appIdHash;
    }

    public void setAppIdHash(String appIdHash) {
        this.appIdHash = appIdHash;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }

    public boolean isOauth2Enabled() {
        return oauth2Enabled;
    }

    public void setOauth2Enabled(boolean oauth2Enabled) {
        this.oauth2Enabled = oauth2Enabled;
    }

    public RegisteredApp.OAuth2ClientType getOauth2ClientType() {
        return oauth2ClientType;
    }

    public void setOauth2ClientType(RegisteredApp.OAuth2ClientType oauth2ClientType) {
        this.oauth2ClientType = oauth2ClientType;
    }

    @Override
    public boolean isNew() {
        return this.getCreatedDate() == null;
    }
}
