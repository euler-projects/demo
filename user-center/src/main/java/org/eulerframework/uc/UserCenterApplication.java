package org.eulerframework.uc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collection;

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

    //@Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> auth2TokenCustomizer() {
        return context -> {
            boolean includePrincipal = false;
            Object rawScope = context.get("org.springframework.security.oauth2.server.authorization.OAuth2Authorization.AUTHORIZED_SCOPE");
            if (rawScope instanceof Collection<?> scopes) {
                includePrincipal = scopes.contains("principal");
            }

            if (includePrincipal) {
                Object principal = context.get("org.springframework.security.core.Authentication.PRINCIPAL");
                if (principal instanceof UsernamePasswordAuthenticationToken token) {
                    if (token.getPrincipal() instanceof UserDetails userDetails) {
                        context.getClaims().claim("principal", userDetails);
                    }
                }
            }
        };
    }
}
