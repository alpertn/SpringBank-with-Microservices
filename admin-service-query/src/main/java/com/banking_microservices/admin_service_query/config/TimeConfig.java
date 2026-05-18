package com.banking_microservices.admin_service_query.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Configuration
public class TimeConfig {

    @Bean
    public Supplier<String> currentTime() {
        return () -> LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
