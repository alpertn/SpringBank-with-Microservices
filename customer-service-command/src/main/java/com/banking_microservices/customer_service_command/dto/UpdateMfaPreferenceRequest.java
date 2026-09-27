package com.banking_microservices.customer_service_command.dto;

import com.banking_microservices.customer_service_command.dto.enums.MfaMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateMfaPreferenceRequest(
        @NotNull UUID customerId,
        boolean mfaEnabled,
        @NotNull MfaMethod mfaMethod
) {
}
