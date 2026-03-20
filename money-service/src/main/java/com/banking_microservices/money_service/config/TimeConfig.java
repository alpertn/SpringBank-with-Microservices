package com.banking_microservices.money_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Configuration
public class TimeConfig {

    @Bean
    public Supplier<String> currentTime() {
        return () -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
