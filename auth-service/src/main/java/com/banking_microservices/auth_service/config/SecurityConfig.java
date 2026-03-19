package com.banking_microservices.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 *
 * Auth Service icin Spring Security Config
 *
 * <p> Auth Service Spring Securityi kullandigi icin otomatik olarak spring security kullanicidan oturum atcmasini ister.
 * Ama Auth service sadece token uretimi token sorgu register login islemleri icin kullanilir
 * O yuzden auth servicede SecurityFilterChaine dokunmuyor Tum istekleri  auth.anyRequest().permitAll()) ile onayliyor
 * cunku Kontrol Gateway'de yapılmıs gelıyor. <p>
 *
 *
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     *
     * Auth Service Mikroservisi icin {@link SecurityFilterChain} bean'ı tanımlar.
     *
     * @param http HttpSecurity  yanı Sprıng Securıty Http yapılandırması. bıze parametre olarak verıyor Sprıng securıty bızde ayarlarını yapıyoruz. ve en sonunda http.build() yazarak devam ettırıyoruz chaını.
     * @return {@link SecurityFilterChain}
     * @throws Exception döndürür. Düz internal server error.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
