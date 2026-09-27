package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateContactVerificationRequest(
        @NotNull UUID customerId,
        boolean emailVerified,
        boolean phoneVerified
) {
}
