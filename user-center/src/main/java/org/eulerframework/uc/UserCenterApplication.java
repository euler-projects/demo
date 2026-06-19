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
import org.eulerframework.resource.Tag;
import org.eulerframework.security.authentication.appattest.AppAttestAttestationRegistration;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.oauth2.core.oidc.EulerOidcClaimNames;
import org.eulerframework.security.oauth2.core.oidc.EulerOidcScopes;
import org.eulerframework.security.oauth2.server.authorization.web.EulerOAuth2AttestationBasedClientAuthenticationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableJpaAuditing
@EnableMethodSecurity
public class UserCenterApplication {

    static void main(String[] args) {
        SpringApplication.run(UserCenterApplication.class, args);
    }

    /**
     * CORS policy aligned with the project's four-quadrant security routing:
     *
     * <ol>
     *   <li>{@code /admin/api/**} &amp; {@code /admin/console/**} &mdash;
     *       admin console XHR and SPA shell. <em>Not registered</em> here so
     *       the browser's same-origin policy fully applies; cross-origin
     *       requests are blocked outright. The admin console is expected to
     *       be served from the same origin as the backend.</li>
     *   <li>{@code /api/**} &mdash; public Bearer-only resource APIs. Open to
     *       any origin but credentials are <em>never</em> echoed back, so
     *       browsers cross-origin calls cannot piggyback on a victim's
     *       session cookies; only callers that can produce a valid Bearer
     *       token will succeed.</li>
     *   <li>{@code /oauth2/token}, {@code /oauth2/revoke},
     *       {@code /oauth2/introspect}, {@code /oauth2/jwks},
     *       {@code /oauth2/device_authorization}, {@code /oauth2/userinfo},
     *       {@code /oauth2/challenge} and {@code /.well-known/**} &mdash;
     *       OAuth2 / OIDC protocol endpoints invoked by SPAs, mobile and
     *       service-to-service clients. Open to any origin, credentials
     *       disabled (these endpoints authenticate via Authorization header
     *       or request body, never via ambient cookies).</li>
     *   <li>{@code /oauth2/authorize}, {@code /login}, {@code /logout} and
     *       other browser-redirect endpoints &mdash; <em>not registered</em>
     *       because top-level navigations are not subject to CORS in the
     *       first place; adding them would only widen the attack surface.</li>
     * </ol>
     *
     * <p>The {@code /_csrf} endpoint is intentionally <em>not registered</em>
     * either: it is only useful from the same-origin admin console, and
     * keeping it out of the CORS source ensures the browser's same-origin
     * policy prevents any third-party page from reading the CSRF token.</p>
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterFilterRegistrationBean() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Public Bearer-only APIs and OAuth2/OIDC protocol endpoints share the
        // same "open but credential-less" policy. Headers are deliberately
        // wildcarded so individual endpoints may accept arbitrary custom
        // request/response headers (tracing, idempotency keys, pagination
        // counters, etc.) without amending the central CORS bean.
        //
        // Two caveats worth keeping in mind:
        //  * "Authorization" is a CORS-defined forbidden header name and is
        //    NOT covered by the "*" wildcard, so it must be listed
        //    explicitly. Safe because AllowCredentials is false.
        //  * "Access-Control-Expose-Headers: *" is supported by all evergreen
        //    browsers when credentials are disabled (Fetch standard, 2020+).
        //    Safari <= 14 ignores it; if any caller still relies on those
        //    legacy versions, switch to an explicit allowlist.
        CorsConfiguration openBearer = new CorsConfiguration();
        openBearer.addAllowedOriginPattern("*");
        openBearer.setAllowedMethods(List.of("*"));
        openBearer.setAllowedHeaders(List.of("Authorization", "*"));
        openBearer.setExposedHeaders(List.of("*"));
        openBearer.setAllowCredentials(false);
        openBearer.setMaxAge(3600L);

        // Public resource APIs (Bearer-only).
        source.registerCorsConfiguration("/api/**", openBearer);

        // OAuth2 / OIDC protocol endpoints invoked as XHR. /oauth2/authorize,
        // /login and /logout are deliberately omitted because they are
        // accessed via top-level navigation, which is not subject to CORS.
        source.registerCorsConfiguration("/oauth2/token", openBearer);
        source.registerCorsConfiguration("/oauth2/revoke", openBearer);
        source.registerCorsConfiguration("/oauth2/introspect", openBearer);
        source.registerCorsConfiguration("/oauth2/jwks", openBearer);
        source.registerCorsConfiguration("/oauth2/device_authorization", openBearer);
        source.registerCorsConfiguration("/oauth2/device_verification", openBearer);
        source.registerCorsConfiguration("/oauth2/userinfo", openBearer);
        source.registerCorsConfiguration("/oauth2/challenge", openBearer);
        source.registerCorsConfiguration("/.well-known/**", openBearer);
        source.registerCorsConfiguration("/otp/**", openBearer);

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
            boolean includeEulerOidcClaims = false;
            if (context.get(AUTHORIZED_SCOPE_KEY) instanceof Collection<?> scopes) {
                includeAuthorities = scopes.contains(EulerOidcScopes.AUTHORITIES);
                includeEulerOidcClaims = scopes.contains(OidcScopes.OPENID);
            }

            if (includeEulerOidcClaims) {
                if (context.get(PRINCIPAL_AUTHENTICATION_KEY) instanceof UsernamePasswordAuthenticationToken token
                        && token.getPrincipal() instanceof EulerUserDetails eulerUserDetails) {
                    List<Tag> tags = eulerUserDetails.getTags();
                    if (tags != null && !tags.isEmpty()) {
                        List<Map<String, String>> tagClaim = tags.stream()
                                .map(tag -> tag.value() == null
                                        ? Map.of(EulerOidcClaimNames.TAGS_KEY, tag.key())
                                        : Map.of(
                                                EulerOidcClaimNames.TAGS_KEY, tag.key(),
                                                EulerOidcClaimNames.TAGS_VALUE, tag.value()))
                                .toList();
                        context.getClaims().claim(EulerOidcClaimNames.TAGS, tagClaim);
                    }
                }
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
