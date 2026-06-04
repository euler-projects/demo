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

package org.eulerframework.uc;

import org.eulerframework.common.util.StringUtils;
import org.eulerframework.security.authentication.appattest.AppAttestAttestationRegistration;
import org.eulerframework.security.oauth2.core.oidc.EulerOidcClaimNames;
import org.eulerframework.security.oauth2.core.oidc.EulerOidcScopes;
import org.eulerframework.security.oauth2.server.authorization.web.EulerOAuth2AttestationBasedClientAuthenticationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableJpaAuditing
@EnableMethodSecurity
public class UserCenterApplication {

    static void main(String[] args) {
        SpringApplication.run(UserCenterApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterFilterRegistrationBean() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/oauth2/**", config);
        source.registerCorsConfiguration("/api/**", config);
        config.setMaxAge(3600L);
        FilterRegistrationBean<CorsFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new CorsFilter(source));
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }

    private static final String PRINCIPAL_AUTHENTICATION_KEY = Authentication.class.getName().concat(".PRINCIPAL");

    private static final String AUTHORIZED_SCOPE_KEY = OAuth2Authorization.class.getName()
            .concat(".AUTHORIZED_SCOPE");

    /**
     * Apple App Attest AAGUID for production environment.
     */
    private static final byte[] PRODUCTION_AAGUID = {
            'a', 'p', 'p', 'a', 't', 't', 'e', 's', 't',
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    /**
     * Apple App Attest AAGUID for development environment.
     */
    private static final byte[] DEVELOPMENT_AAGUID = {
            'a', 'p', 'p', 'a', 't', 't', 'e', 's', 't',
            'd', 'e', 'v', 'e', 'l', 'o', 'p'
    };

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> auth2TokenCustomizer() {
        return context -> {
            boolean includeAuthorities = false;
            //boolean includeEulerOidcClaims = false;
            if (context.get(AUTHORIZED_SCOPE_KEY) instanceof Collection<?> scopes) {
                includeAuthorities = scopes.contains(EulerOidcScopes.AUTHORITIES);
                //includeEulerOidcClaims = scopes.contains(OidcScopes.OPENID);
            }

            if (includeAuthorities) {
                if (context.get(PRINCIPAL_AUTHENTICATION_KEY) instanceof UsernamePasswordAuthenticationToken token
                        && token.getPrincipal() instanceof UserDetails userDetails) {
                    Set<String> authorities = userDetails.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(StringUtils::hasText)
                            .collect(Collectors.toSet());
                    if (!CollectionUtils.isEmpty(authorities)) {
                        context.getClaims().claim(EulerOidcClaimNames.AUTHORITIES, authorities);
                    }
                }
            }

            // Inject verified app metadata from Apple App Attest attestation into the JWT access token.
            // When present, the resulting JWT will contain a "vapp" (verified app) claim, e.g.:
            //
            //   {
            //     "vapp": {
            //       "bid": "com.example.MyApp",
            //       "env": 0
            //     }
            //   }
            //
            // Where "bid" is the app's bundle identifier, and "env" indicates the attestation
            // environment: 0 = production, 1 = development.
            if (context.getAuthorizationGrant() instanceof OAuth2AuthorizationGrantAuthenticationToken grant) {
                Object value = grant.getAdditionalParameters().get(
                        EulerOAuth2AttestationBasedClientAuthenticationFilter.VERIFIED_CLIENT_ATTESTATION_PARAMETER);
                if (value instanceof AppAttestAttestationRegistration reg) {
                    String bundleId = reg.getBundleId();
                    byte[] aaguid = reg.getAaguid();
                    Map<String, Object> verifiedAppMetadata = new HashMap<>(4);
                    if (StringUtils.hasText(bundleId)) {
                        verifiedAppMetadata.put(EulerOidcClaimNames.VERIFIED_APP_METADATA_BUNDLE_ID, bundleId);
                    }
                    if (aaguid != null) {
                        if (MessageDigest.isEqual(aaguid, PRODUCTION_AAGUID)) {
                            verifiedAppMetadata.put(EulerOidcClaimNames.VERIFIED_APP_METADATA_ENVIRONMENT, 0);
                        } else if (MessageDigest.isEqual(aaguid, DEVELOPMENT_AAGUID)) {
                            verifiedAppMetadata.put(EulerOidcClaimNames.VERIFIED_APP_METADATA_ENVIRONMENT, 1);
                        }
                    }
                    if (!verifiedAppMetadata.isEmpty()) {
                        context.getClaims().claim(EulerOidcClaimNames.VERIFIED_APP_METADATA, verifiedAppMetadata);
                    }
                }
            }
        };
    }
}
