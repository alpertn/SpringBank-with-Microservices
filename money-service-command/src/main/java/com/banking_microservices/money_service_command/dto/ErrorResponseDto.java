package com.banking_microservices.money_service_command.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String message,
        String errorCode,
        LocalDateTime timestamp
) {
}
