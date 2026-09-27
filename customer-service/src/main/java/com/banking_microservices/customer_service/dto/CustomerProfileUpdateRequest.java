package com.banking_microservices.customer_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerProfileUpdateRequest(
        @NotBlank String id,
        @Email @NotBlank String email,
        String phoneNumber,
        String birthdate,
        @NotBlank String name,
        String middleName,
        @NotBlank String surname,
        String sex,
        String preferredLanguage,
        String nationalityCode
) {
}
