package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.CreateCustomerCommandRequest;
import com.banking_microservices.customer_service_command.dto.UpdateContactVerificationRequest;
import com.banking_microservices.customer_service_command.dto.UpdateCustomerProfileRequest;
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
import com.banking_microservices.customer_service_command.model.Customer;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerProfileService {
    private final NationalIdProtector nationalIdProtector;
    private final CustomerAddressService addressService;

    public Customer createModel(CreateCustomerCommandRequest request) {
        NationalIdProtector.ProtectedNationalId protectedId = nationalIdProtector.protect(request.nationalId());
        return Customer.builder()
                .keycloakId(request.keycloakId())
                .realm(request.realm() == null ? KeycloakRealm.BANKING : request.realm())
                .email(normalizeEmail(request.email()))
                .emailVerified(request.emailVerified())
                .phoneNumber(blankToNull(request.phoneNumber()))
                .phoneVerified(request.phoneVerified())
                .userType(request.userType() == null ? UserType.INDIVIDUAL : request.userType())
                .status(request.status() == null ? UserStatus.ACTIVE : request.status())
                .birthdate(request.birthdate())
                .name(request.name().trim())
                .middleName(blankToNull(request.middleName()))
                .surname(request.surname().trim())
                .sex(request.sex() == null ? Sex.UNSPECIFIED : request.sex())
                .riskScore(request.riskScore())
                .mfaEnabled(request.mfaEnabled())
                .mfaMethod(request.mfaMethod() == null ? MfaMethod.NONE : request.mfaMethod())
                .preferredLanguage(blankToDefault(request.preferredLanguage(), "tr"))
                .specialCustomer(request.specialCustomer())
                .specialCustomerScore(request.specialCustomerScore())
                .kycStatus(request.kycStatus() == null ? KycStatus.PENDING : request.kycStatus())
                .nationalityCode(request.nationalityCode() == null ? NationalityCode.TR : request.nationalityCode())
                .nationalIdHash(protectedId.hash())
                .nationalIdMasked(protectedId.masked())
                .accountStatus(request.accountStatus() == null ? AccountStatus.PENDING_ACTIVATION : request.accountStatus())
                .maritalStatus(request.maritalStatus() == null ? MaritalStatus.UNKNOWN : request.maritalStatus())
                .riskClass(request.riskClass() == null ? RiskClass.RISK_1 : request.riskClass())
                .segment(request.segment() == null ? CustomerSegment.STANDARD : request.segment())
                .segmentScore(request.segmentScore() == 0 ? 1 : request.segmentScore())
                .politicalExposureStatus(request.politicalExposureStatus() == null
                        ? PoliticalExposureStatus.NONE : request.politicalExposureStatus())
                .employmentCategory(request.employmentCategory() == null
                        ? EmploymentCategory.OTHER : request.employmentCategory())
                .address(addressService.parse(request.address()))
                .build();
    }

    public void updateProfile(Customer customer, UpdateCustomerProfileRequest request) {
        if (request.email() != null && !request.email().isBlank()) customer.setEmail(normalizeEmail(request.email()));
        if (request.phoneNumber() != null) customer.setPhoneNumber(blankToNull(request.phoneNumber()));
        if (request.userType() != null) customer.setUserType(request.userType());
        if (request.birthdate() != null) customer.setBirthdate(request.birthdate());
        if (request.name() != null && !request.name().isBlank()) customer.setName(request.name().trim());
        if (request.middleName() != null) customer.setMiddleName(blankToNull(request.middleName()));
        if (request.surname() != null && !request.surname().isBlank()) customer.setSurname(request.surname().trim());
        if (request.sex() != null) customer.setSex(request.sex());
        if (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) {
            customer.setPreferredLanguage(request.preferredLanguage().trim());
        }
        if (request.nationalityCode() != null) customer.setNationalityCode(request.nationalityCode());
    }

    public void updateContactVerification(Customer customer, UpdateContactVerificationRequest request) {
        customer.setEmailVerified(request.emailVerified());
        customer.setPhoneVerified(request.phoneVerified());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
