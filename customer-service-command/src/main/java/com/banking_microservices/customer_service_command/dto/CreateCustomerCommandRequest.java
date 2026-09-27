package com.banking_microservices.customer_service_command.dto;

import com.banking_microservices.customer_service_command.dto.enums.KeycloakRealm;
import com.banking_microservices.customer_service_command.dto.enums.AccountStatus;
import com.banking_microservices.customer_service_command.dto.enums.CustomerSegment;
import com.banking_microservices.customer_service_command.dto.enums.EmploymentCategory;
import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import com.banking_microservices.customer_service_command.dto.enums.MfaMethod;
import com.banking_microservices.customer_service_command.dto.enums.NationalityCode;
import com.banking_microservices.customer_service_command.dto.enums.MaritalStatus;
import com.banking_microservices.customer_service_command.dto.enums.PoliticalExposureStatus;
import com.banking_microservices.customer_service_command.dto.enums.RiskClass;
import com.banking_microservices.customer_service_command.dto.enums.Sex;
import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import com.banking_microservices.customer_service_command.dto.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CreateCustomerCommandRequest(
        @NotNull UUID keycloakId,
        KeycloakRealm realm,
        @Email @NotBlank String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,
        UserType userType,
        UserStatus status,
        LocalDateTime birthdate,
        @NotBlank String name,
        String middleName,
        @NotBlank String surname,
        Sex sex,
        @Min(0) @Max(100) int riskScore,
        boolean mfaEnabled,
        MfaMethod mfaMethod,
        String preferredLanguage,
        boolean specialCustomer,
        @Min(0) @Max(100) int specialCustomerScore,
        KycStatus kycStatus,
        NationalityCode nationalityCode,
        @NotBlank String nationalId,
        AccountStatus accountStatus,
        MaritalStatus maritalStatus,
        RiskClass riskClass,
        CustomerSegment segment,
        @Min(1) @Max(10) int segmentScore,
        PoliticalExposureStatus politicalExposureStatus,
        EmploymentCategory employmentCategory,
        @NotNull AddressInput address
) {
}
