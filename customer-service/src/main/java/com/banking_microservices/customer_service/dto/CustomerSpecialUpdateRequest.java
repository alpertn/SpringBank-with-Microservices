package com.banking_microservices.customer_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CustomerSpecialUpdateRequest(@NotBlank String id, boolean specialCustomer, @Min(0) @Max(100) int specialCustomerScore) {
}
