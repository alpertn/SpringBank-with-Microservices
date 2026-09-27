package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerMfaUpdateRequest(
        @NotBlank String id,
        boolean mfaEnabled,
        @NotBlank String mfaMethod
) {
}
