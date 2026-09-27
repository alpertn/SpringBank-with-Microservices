package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SpecialCustomerRequest(
        @NotNull UUID customerId,
        boolean specialCustomer,
        @Min(0) @Max(100) int specialCustomerScore
) {
}
