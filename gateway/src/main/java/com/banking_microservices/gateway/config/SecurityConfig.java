package com.banking_microservices.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

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


    }
}
