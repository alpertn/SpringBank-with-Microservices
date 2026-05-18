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

/**
 * Gateway uzerindeki tum Spring Security ve WebFlux guvenlik yapilandirmalarinin
 * yapildigi siniftir.
 * 
 * Hangi yollarin (path) public, hangilerinin authentication gerektirdigini,
 * JWT token kontrolunu ve rollerin nasil cikarilacagini belirler.
 *
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Gelen Spring WebFlux istekleri icin guvenlik filtre zincirini olusturur.
     * Statik sayfalar ve bazi API'ler public yapilmistir.
     * 
     * @param httpSecurity {@link ServerHttpSecurity} guvenlik yapilandirmasi
     * @return {@link SecurityWebFilterChain} secilen kurallara gore filtre zinciri dondurur.
     */
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
                        .pathMatchers("/api/user-service/v1/auth/**").permitAll() // new auth path
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll() // gateway health check public
                        // Servis actuator health endpoint'leri - admin paneli için permitAll
                        .pathMatchers("/api/user-service/actuator/**").permitAll()
                        .pathMatchers("/api/money-service/actuator/**").permitAll()
                        .pathMatchers("/api/transaction-service/actuator/**").permitAll()
                        .pathMatchers("/api/fraud-service/actuator/**").permitAll()
                        .pathMatchers("/api/admin-service/actuator/**").permitAll()
                        .pathMatchers("/api/admin-service-command/actuator/**").permitAll()
                        .pathMatchers("/api/admin-service-query/actuator/**").permitAll()
                        .pathMatchers("/api/user-service/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/money-service/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/transaction-service/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/fraud-service/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/admin-service/**").hasRole("ADMIN")
                        .pathMatchers("/api/admin-service-command/**").hasRole("ADMIN")
                        .pathMatchers("/api/admin-service-query/**").hasRole("ADMIN")
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

    /**
     * Keycloaktan gelen JWT tokenini Spring Security'nin anlayacagi formata cevirir.
     * İcindeki rolleri ayiklayip Authority listesine ekler.
     *
     * @return {@link Converter} JWT'yi {@link AbstractAuthenticationToken} objesine dondurur.
     */
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
