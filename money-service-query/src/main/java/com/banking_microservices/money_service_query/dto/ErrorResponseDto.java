package com.banking_microservices.money_service_query.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String message,
        String errorCode,
        LocalDateTime timestamp
) {
}
