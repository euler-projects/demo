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

package org.eulerframework.uc.oauth2.model;

import org.eulerframework.security.oauth2.server.authorization.settings.EulerOAuth2TokenSettings;

/**
 * Transport projection of {@link EulerOAuth2TokenSettings}, shared by both the
 * request and response DTOs of the Admin OAuth 2.0 Client API.
 *
 * <p>Time-to-live values are exposed as {@link Long} numbers of <b>seconds</b>,
 * matching the canonical storage form chosen by {@link EulerOAuth2TokenSettings}
 * and keeping the JSON representation stable across Jackson's integer-width
 * heuristics.
 */
public class OAuth2TokenSettingsDto {

    private Long authorizationCodeTimeToLive;
    private Long accessTokenTimeToLive;
    private String accessTokenFormat;
    private Long deviceCodeTimeToLive;
    private Boolean reuseRefreshTokens;
    private Long refreshTokenTimeToLive;

    public Long getAuthorizationCodeTimeToLive() {
        return authorizationCodeTimeToLive;
    }

    public void setAuthorizationCodeTimeToLive(Long authorizationCodeTimeToLive) {
        this.authorizationCodeTimeToLive = authorizationCodeTimeToLive;
    }

    public Long getAccessTokenTimeToLive() {
        return accessTokenTimeToLive;
    }

    public void setAccessTokenTimeToLive(Long accessTokenTimeToLive) {
        this.accessTokenTimeToLive = accessTokenTimeToLive;
    }

    public String getAccessTokenFormat() {
        return accessTokenFormat;
    }

    public void setAccessTokenFormat(String accessTokenFormat) {
        this.accessTokenFormat = accessTokenFormat;
    }

    public Long getDeviceCodeTimeToLive() {
        return deviceCodeTimeToLive;
    }

    public void setDeviceCodeTimeToLive(Long deviceCodeTimeToLive) {
        this.deviceCodeTimeToLive = deviceCodeTimeToLive;
    }

    public Boolean getReuseRefreshTokens() {
        return reuseRefreshTokens;
    }

    public void setReuseRefreshTokens(Boolean reuseRefreshTokens) {
        this.reuseRefreshTokens = reuseRefreshTokens;
    }

    public Long getRefreshTokenTimeToLive() {
        return refreshTokenTimeToLive;
    }

    public void setRefreshTokenTimeToLive(Long refreshTokenTimeToLive) {
        this.refreshTokenTimeToLive = refreshTokenTimeToLive;
    }

    /**
     * Materializes an {@link EulerOAuth2TokenSettings} from this DTO, delegating
     * to the builder so that value normalization and canonical defaults are
     * applied consistently.
     *
     * @return the populated settings
     */
    public EulerOAuth2TokenSettings toModel() {
        return EulerOAuth2TokenSettings.builder()
                .authorizationCodeTimeToLive(this.authorizationCodeTimeToLive)
                .accessTokenTimeToLive(this.accessTokenTimeToLive)
                .accessTokenFormat(this.accessTokenFormat)
                .deviceCodeTimeToLive(this.deviceCodeTimeToLive)
                .reuseRefreshTokens(this.reuseRefreshTokens)
                .refreshTokenTimeToLive(this.refreshTokenTimeToLive)
                .build();
    }

    /**
     * Projects an {@link EulerOAuth2TokenSettings} into its transport form.
     *
     * @param settings the domain settings, or {@code null}
     * @return the DTO, or {@code null} when the source is {@code null}
     */
    public static OAuth2TokenSettingsDto fromModel(EulerOAuth2TokenSettings settings) {
        if (settings == null) {
            return null;
        }
        OAuth2TokenSettingsDto dto = new OAuth2TokenSettingsDto();
        dto.authorizationCodeTimeToLive = settings.getAuthorizationCodeTimeToLive();
        dto.accessTokenTimeToLive = settings.getAccessTokenTimeToLive();
        dto.accessTokenFormat = settings.getAccessTokenFormat();
        dto.deviceCodeTimeToLive = settings.getDeviceCodeTimeToLive();
        dto.reuseRefreshTokens = settings.getReuseRefreshTokens();
        dto.refreshTokenTimeToLive = settings.getRefreshTokenTimeToLive();
        return dto;
    }
}
