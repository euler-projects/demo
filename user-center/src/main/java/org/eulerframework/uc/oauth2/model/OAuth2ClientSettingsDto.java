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

import org.eulerframework.security.oauth2.server.authorization.settings.EulerOAuth2ClientSettings;

/**
 * Transport projection of {@link EulerOAuth2ClientSettings}, shared by both the
 * request and response DTOs of the Admin OAuth 2.0 Client API.
 *
 * <p>These properties fall outside of RFC&nbsp;7591 and therefore have no
 * standardized wire format; flattening them onto a dedicated transport object
 * keeps the JSON schema stable and shields the API contract from the
 * {@code Map<String, Object>} backing of {@code AbstractSettings}.
 */
public class OAuth2ClientSettingsDto {

    private Boolean requireProofKey;
    private Boolean requireAuthorizationConsent;

    public Boolean getRequireProofKey() {
        return requireProofKey;
    }

    public void setRequireProofKey(Boolean requireProofKey) {
        this.requireProofKey = requireProofKey;
    }

    public Boolean getRequireAuthorizationConsent() {
        return requireAuthorizationConsent;
    }

    public void setRequireAuthorizationConsent(Boolean requireAuthorizationConsent) {
        this.requireAuthorizationConsent = requireAuthorizationConsent;
    }

    /**
     * Materializes an {@link EulerOAuth2ClientSettings} from this DTO, delegating
     * to the builder so that canonical defaults are applied to any omitted entry.
     *
     * @return the populated settings
     */
    public EulerOAuth2ClientSettings toModel() {
        return EulerOAuth2ClientSettings.builder()
                .requireProofKey(this.requireProofKey)
                .requireAuthorizationConsent(this.requireAuthorizationConsent)
                .build();
    }

    /**
     * Projects an {@link EulerOAuth2ClientSettings} into its transport form.
     *
     * @param settings the domain settings, or {@code null}
     * @return the DTO, or {@code null} when the source is {@code null}
     */
    public static OAuth2ClientSettingsDto fromModel(EulerOAuth2ClientSettings settings) {
        if (settings == null) {
            return null;
        }
        OAuth2ClientSettingsDto dto = new OAuth2ClientSettingsDto();
        dto.requireProofKey = settings.getRequireProofKey();
        dto.requireAuthorizationConsent = settings.getRequireAuthorizationConsent();
        return dto;
    }
}
