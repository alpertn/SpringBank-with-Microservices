package com.banking_microservices.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * GÜVENLİK YAPILANDIRMASI
 * ------------------------
 * Bu sınıf Gateway'e gelen istekleri kontrol eder:
 * 1. Kullanıcı giriş yapmış mı? (JWT token var mı?)
 * 2. Kullanıcının yetkisi var mı? (ADMIN rolü var mı?)
 *
 * NOT: Keycloak'tan gelen JWT token'ı okur ve Spring Security'ye çevirir
 */
@Configuration  // Spring'e "Bu bir yapılandırma sınıfıdır" der
@EnableWebFluxSecurity  // WebFlux (reactive) güvenlik sistemini aktif eder
public class SecurityConfig {

    /**
     * ANA GÜVENLİK KURALLARI
     * -----------------------
     * Hangi URL'ye kim erişebilir?
     */
    @Bean  // Spring bu metodu çalıştırıp sonucu saklayacak
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF korumasını kapat (API'lerde genelde kapalı olur)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // URL ERİŞİM KURALLARI
                .authorizeExchange(auth -> auth
                        // Bu URL'lere herkes erişebilir (giriş yapmadan)
                        .pathMatchers("/api/auth/**", "/actuator/**").permitAll()

                        // Bu URL'lere sadece ADMIN rolü olanlar erişebilird
                        .pathMatchers("/api/admin/**", "/api/users").hasRole("ADMIN")

                        // Yukarıdakiler dışında kalan BÜTÜN URL'ler için
                        // kullanıcı giriş yapmış olmalı (authenticated)
                        .anyExchange().authenticated())

                // JWT TOKEN KONTROLÜ
                // Gelen istekteki Authorization: Bearer <token> kısmını kontrol et
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))

                .build();  // Yapılandırmayı tamamla
    }

    /**
     * JWT TOKEN'I SPRING SECURITY'YE ÇEVİRME
     * ---------------------------------------
     * Keycloak'tan gelen JWT içindeki rolleri Spring'in anlayacağı hale getirir
     *
     * Örnek JWT içeriği:
     * {
     *   "sub": "user123",
     *   "realm_access": {
     *     "roles": ["admin", "user"]
     *   }
     * }
     *
     * Bu metot "admin" rolünü "ROLE_ADMIN" yapar
     */
    private ReactiveJwtAuthenticationConverter jwtConverter() {
        // JWT'yi okuyup Spring Authentication objesine çeviren sınıf
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();

        // JWT içindeki rolleri nasıl çıkaracağımızı belirtiyoruz
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            // 1. ADIM: JWT'den "realm_access" bölümünü al
            // Bu Keycloak'ın rolleri sakladığı yer
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            // Eğer realm_access yoksa, boş liste döndür (kullanıcının rolü yok)
            if (realmAccess == null) return Flux.empty();

            // 2. ADIM: realm_access içinden "roles" listesini al
            @SuppressWarnings("unchecked")  // Java'ya "Bu cast güvenli" diyoruz
            List<String> roles = (List<String>) realmAccess.get("roles");

            // Eğer roles yoksa, boş liste döndür
            if (roles == null) return Flux.empty();

            // 3. ADIM: Her rolü Spring Security formatına çevir
            // Örnek: "admin" -> "ROLE_ADMIN"
            return Flux.fromIterable(roles)  // Listeyi Flux'a çevir (reactive akış)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            //       ↑
            //   Her rol için "ROLE_" ekle ve büyük harfe çevir
        });

        return converter;
    }
}
