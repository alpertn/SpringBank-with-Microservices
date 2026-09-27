package com.banking_microservices.customer_service_command.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String message,
        String errorCode,
        LocalDateTime timestamp
) {
}
