package com.banking_microservices.gateway.config;

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

    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity){
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(authorizeExchange -> authorizeExchange
                        .pathMatchers("/api/auth/**").permitAll() // public
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated()) // geri kalani icin sadece login olmasi yeterli olsun
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(JwtConverter)))
            //    .build()


    }

//    // Keycloak JWT -> Spring Security Authority dönüşümü
//    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
//        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
//        converter.setJwtGrantedAuthoritiesConverter(this::extractRoles);
//        return new ReactiveJwtAuthenticationConverterAdapter(converter);
//    }
//
//    // realm_access.roles -> ROLE_XXX
//    @SuppressWarnings("unchecked")
//    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
//        Map<String, Object> realm = jwt.getClaim("realm_access");
//        if (realm == null)
//            return List.of();
//
//        List<String> roles = (List<String>) realm.get("roles");
//        if (roles == null)
//            return List.of();
//
//        return roles.stream()
//                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
//                .toList();
//    }

}
