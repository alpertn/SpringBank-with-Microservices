package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerStatusUpdateRequest(
        @NotBlank String id,
        @NotBlank String status
) {
}
