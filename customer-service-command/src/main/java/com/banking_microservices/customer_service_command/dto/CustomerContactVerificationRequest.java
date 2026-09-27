package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerContactVerificationRequest(
        @NotBlank String id,
        boolean emailVerified,
        boolean phoneVerified
) {
}
