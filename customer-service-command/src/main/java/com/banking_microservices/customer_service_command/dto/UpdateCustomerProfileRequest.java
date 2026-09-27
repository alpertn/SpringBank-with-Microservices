package com.banking_microservices.customer_service_command.dto;

import com.banking_microservices.customer_service_command.dto.enums.NationalityCode;
import com.banking_microservices.customer_service_command.dto.enums.Sex;
import com.banking_microservices.customer_service_command.dto.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UpdateCustomerProfileRequest(
        @NotNull UUID customerId,
        @Email String email,
        String phoneNumber,
        UserType userType,
        LocalDateTime birthdate,
        String name,
        String middleName,
        String surname,
        Sex sex,
        String preferredLanguage,
        NationalityCode nationalityCode
) {
}
