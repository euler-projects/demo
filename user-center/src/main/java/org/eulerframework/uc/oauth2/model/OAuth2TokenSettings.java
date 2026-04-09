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

public class OAuth2TokenSettings implements EulerOAuth2TokenSettings {

    private Long authorizationCodeTimeToLive;
    private Long accessTokenTimeToLive;
    private String accessTokenFormat;
    private Long deviceCodeTimeToLive;
    private Boolean reuseRefreshTokens;
    private Long refreshTokenTimeToLive;

    @Override
    public Long getAuthorizationCodeTimeToLive() {
        return authorizationCodeTimeToLive;
    }

    public void setAuthorizationCodeTimeToLive(Long authorizationCodeTimeToLive) {
        this.authorizationCodeTimeToLive = authorizationCodeTimeToLive;
    }

    @Override
    public Long getAccessTokenTimeToLive() {
        return accessTokenTimeToLive;
    }

    public void setAccessTokenTimeToLive(Long accessTokenTimeToLive) {
        this.accessTokenTimeToLive = accessTokenTimeToLive;
    }

    @Override
    public String getAccessTokenFormat() {
        return accessTokenFormat;
    }

    public void setAccessTokenFormat(String accessTokenFormat) {
        this.accessTokenFormat = accessTokenFormat;
    }

    @Override
    public Long getDeviceCodeTimeToLive() {
        return deviceCodeTimeToLive;
    }

    public void setDeviceCodeTimeToLive(Long deviceCodeTimeToLive) {
        this.deviceCodeTimeToLive = deviceCodeTimeToLive;
    }

    @Override
    public Boolean getReuseRefreshTokens() {
        return reuseRefreshTokens;
    }

    public void setReuseRefreshTokens(Boolean reuseRefreshTokens) {
        this.reuseRefreshTokens = reuseRefreshTokens;
    }

    @Override
    public Long getRefreshTokenTimeToLive() {
        return refreshTokenTimeToLive;
    }

    public void setRefreshTokenTimeToLive(Long refreshTokenTimeToLive) {
        this.refreshTokenTimeToLive = refreshTokenTimeToLive;
    }
}
