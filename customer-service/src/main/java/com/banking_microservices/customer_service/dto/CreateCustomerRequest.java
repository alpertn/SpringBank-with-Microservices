package com.banking_microservices.customer_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateCustomerRequest(
        @NotBlank String keycloakId,
        @Email @NotBlank String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String birthdate,
        @NotBlank String name,
        String middleName,
        @NotBlank String surname,
        @NotBlank String nationalId,
        String nationalityCode,
        String maritalStatus,
        String accountStatus,
        String riskClass,
        String segment,
        @Min(1) @Max(10) int segmentScore,
        String politicalExposureStatus,
        String employmentCategory,
        @NotNull AddressRequest address
) {
    public CreateCustomerRequest(String keycloakId, String email, boolean emailVerified, String phoneNumber,
                                 boolean phoneVerified, String birthdate, String name, String middleName, String surname) {
        this(keycloakId, email, emailVerified, phoneNumber, phoneVerified, birthdate, name, middleName, surname,
                "10000000146", "TR", "UNKNOWN", "PENDING_ACTIVATION", "RISK_1", "STANDARD", 1,
                "NONE", "OTHER", new AddressRequest("RESIDENTIAL", "Test address", "TR"));
    }
}
