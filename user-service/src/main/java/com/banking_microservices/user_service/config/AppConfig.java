package com.banking_microservices.user_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Genel uygulama konfigurasyonu.
 *
 * ObjectMapper burada manuel olarak tanimlanmistir.
 * Neden: spring-boot-starter-webmvc + spring-boot-starter-webflux birlikte kullanildiginda
 * Spring Boot 4.x'in Jackson auto-configuration'i ObjectMapper bean'ini her zaman olusturmuyor.
 * TokenDecoderService ve diger bilesenler ObjectMapper inject ettiginden bu bean zorunludur.
 *
 * JavaTimeModule: LocalDateTime / LocalDate gibi java.time siniflari Java 17+ moduler sistemi
 * altinda yalın reflection ile erisilemediginden bu modul zorunludur.
 */
@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Java 8+ Date/Time API destegi (LocalDateTime, LocalDate, ZonedDateTime vb.)
        mapper.registerModule(new JavaTimeModule());
        // Tarihleri timestamp yerine ISO-8601 string olarak yaz: "2024-01-15T10:30:00"
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
