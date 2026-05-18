package com.banking_microservices.admin_service_query.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String message,
        String code,
        LocalDateTime timestamp
) {
}
