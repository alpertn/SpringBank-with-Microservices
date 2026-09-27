package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CustomerRiskUpdateRequest(
        @NotBlank String id,
        @Min(0) @Max(100) int riskScore
) {
}
