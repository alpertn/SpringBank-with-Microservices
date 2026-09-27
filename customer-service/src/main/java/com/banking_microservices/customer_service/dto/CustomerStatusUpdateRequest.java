package com.banking_microservices.customer_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerStatusUpdateRequest(@NotBlank String id, @NotBlank String status) {
}
