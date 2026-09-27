package com.banking_microservices.customer_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        String type,
        @NotBlank @Size(max = 1000) String rawAddress,
        @Size(min = 2, max = 3) String countryCode
) {}
