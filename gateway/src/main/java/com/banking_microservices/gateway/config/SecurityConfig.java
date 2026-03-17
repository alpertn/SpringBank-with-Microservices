package com.banking_microservices.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(authorizeExchange -> authorizeExchange
                        .pathMatchers("/", "/index.html", "/login.html", "/register.html",
                                "/dashboard.html", "/transfer.html", "/admin.html",
                                "/deposit.html", "/withdraw.html", "/transactions.html",
                                "/css/**", "/js/**", "/favicon.ico", "/images/**", "/fonts/**")
                        .permitAll() // Tüm statik HTML sayfaları public — auth, API katmanında yapılır.
                        .pathMatchers("/api/auth-service/**").permitAll() // login register public
                        .pathMatchers("/api/user-service/v1/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated()) // geri kalani icin login yeterli olsun demek bu.
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().getHeaders().remove("WWW-Authenticate");
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .build();
    }

    // Keycloak JWT  Spring Security Authority donusum
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractRoles);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    // formatter
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> realm = jwt.getClaim("realm_access");
        if (realm == null)
            return List.of();

        List<String> roles = (List<String>) realm.get("roles");
        if (roles == null)
            return List.of();

        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .toList();
    }
}