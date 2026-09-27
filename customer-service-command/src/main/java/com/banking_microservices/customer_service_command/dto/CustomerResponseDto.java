package com.banking_microservices.customer_service_command.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponseDto(
        UUID id,
        UUID keycloakId,
        String realm,
        String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String userType,
        String status,
        LocalDateTime birthdate,
        String name,
        String middleName,
        String surname,
        String sex,
        int riskScore,
        boolean mfaEnabled,
        String mfaMethod,
        String preferredLanguage,
        boolean specialCustomer,
        int specialCustomerScore,
        String kycStatus,
        String nationalityCode,
        String nationalIdMasked,
        String accountStatus,
        String maritalStatus,
        String riskClass,
        String segment,
        int segmentScore,
        String politicalExposureStatus,
        String employmentCategory,
        AddressDto address,
        boolean deleted,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
