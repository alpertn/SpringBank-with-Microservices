package com.banking_microservices.customer_service_query.dto;

import java.time.LocalDateTime;

public record CustomerReadDto(
        String id,
        String keycloakId,
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
        String lastOperationType,
        LocalDateTime lastSyncedAt
) {
}
