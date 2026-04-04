package org.eulerframework.uc;

import org.eulerframework.common.util.StringUtils;
import org.eulerframework.security.authentication.ChallengeService;
import org.eulerframework.security.authentication.InMemoryChallengeService;
import org.eulerframework.security.oauth2.core.oidc.EulerOidcScopes;
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
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableJpaAuditing
@EnableMethodSecurity
public class UserCenterApplication {

    public static void main(String[] args) {
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

    @Bean
    public ChallengeService challengeService() {
        return new InMemoryChallengeService();
    }


    private static final String PRINCIPAL_AUTHENTICATION_KEY = Authentication.class.getName().concat(".PRINCIPAL");

    private static final String AUTHORIZED_SCOPE_KEY = OAuth2Authorization.class.getName()
            .concat(".AUTHORIZED_SCOPE");

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> auth2TokenCustomizer() {
        return context -> {
            boolean includeAuthorities = false;
            if (context.get(AUTHORIZED_SCOPE_KEY) instanceof Collection<?> scopes) {
                includeAuthorities = scopes.contains(EulerOidcScopes.AUTHORITIES);
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
                        context.getClaims().claim(EulerOidcScopes.AUTHORITIES, authorities);
                    }
                }
            }
        };
    }
}
