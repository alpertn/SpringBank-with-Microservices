package com.banking_microservices.customer_service_command.dto;

import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateCustomerStatusRequest(
        @NotNull UUID customerId,
        @NotNull UserStatus status
) {
}
