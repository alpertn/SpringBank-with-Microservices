package com.banking_microservices.customer_service_command.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank String keycloakId,
        String realm,
        @Email @NotBlank String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String userType,
        String birthdate,
        @NotBlank String name,
        String middleName,
        @NotBlank String surname,
        String sex,
        String preferredLanguage,
        String nationalityCode
) {
}
