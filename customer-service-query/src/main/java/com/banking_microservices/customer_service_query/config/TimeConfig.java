package com.banking_microservices.customer_service_query.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Configuration
public class TimeConfig {

    @Bean
    public Supplier<String> currentTime() {
        return () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
