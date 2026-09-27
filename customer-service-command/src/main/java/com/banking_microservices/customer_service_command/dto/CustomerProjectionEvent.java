package com.banking_microservices.customer_service_command.dto;

import lombok.Builder;

@Builder
public record CustomerProjectionEvent(
        String eventId,
        String aggregateId,
        String keycloakId,
        String realm,
        String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        String userType,
        String status,
        String birthdate,
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
        String operationType,
        String occurredAt,
        String sourceService
) {
}
