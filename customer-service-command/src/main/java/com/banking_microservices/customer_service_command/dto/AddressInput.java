package com.banking_microservices.customer_service_command.dto;

import com.banking_microservices.customer_service_command.dto.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressInput(
        AddressType type,
        @NotBlank @Size(max = 1000) String rawAddress,
        @Size(min = 2, max = 3) String countryCode
) {
}
