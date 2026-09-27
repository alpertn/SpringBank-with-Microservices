package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateRiskScoreRequest(
        @NotNull UUID customerId,
        @Min(0) @Max(100) int riskScore
) {
}
