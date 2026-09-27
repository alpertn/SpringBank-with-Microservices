package com.banking_microservices.customer_service_command.dto;

import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateKycStatusRequest(
        @NotNull UUID customerId,
        @NotNull KycStatus kycStatus
) {
}
