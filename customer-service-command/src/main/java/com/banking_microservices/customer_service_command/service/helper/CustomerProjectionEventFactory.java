package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.CustomerProjectionEvent;
import com.banking_microservices.customer_service_command.model.Customer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class CustomerProjectionEventFactory {

    private final CustomerMapper customerMapper = new CustomerMapper();

    public CustomerProjectionEvent create(Customer customer, String operationType) {
        return CustomerProjectionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateId(customer.getId().toString())
                .keycloakId(customer.getKeycloakId().toString())
                .realm(customer.getRealm().name())
                .email(customer.getEmail())
                .emailVerified(customer.isEmailVerified())
                .phoneNumber(customer.getPhoneNumber())
                .phoneVerified(customer.isPhoneVerified())
                .userType(customer.getUserType().name())
                .status(customer.getStatus().name())
                .birthdate(customer.getBirthdate() == null ? null : customer.getBirthdate().toString())
                .name(customer.getName())
                .middleName(customer.getMiddleName())
                .surname(customer.getSurname())
                .sex(customer.getSex().name())
                .riskScore(customer.getRiskScore())
                .mfaEnabled(customer.isMfaEnabled())
                .mfaMethod(customer.getMfaMethod().name())
                .preferredLanguage(customer.getPreferredLanguage())
                .specialCustomer(customer.isSpecialCustomer())
                .specialCustomerScore(customer.getSpecialCustomerScore())
                .kycStatus(customer.getKycStatus().name())
                .nationalityCode(customer.getNationalityCode().name())
                .nationalIdMasked(customer.getNationalIdMasked())
                .accountStatus(customer.getAccountStatus().name())
                .maritalStatus(customer.getMaritalStatus().name())
                .riskClass(customer.getRiskClass().name())
                .segment(customer.getSegment().name())
                .segmentScore(customer.getSegmentScore())
                .politicalExposureStatus(customer.getPoliticalExposureStatus().name())
                .employmentCategory(customer.getEmploymentCategory().name())
                .address(customerMapper.toResponse(customer).address())
                .deleted(customer.isDeleted())
                .version(customer.getVersion())
                .operationType(operationType)
                .occurredAt(LocalDateTime.now().toString())
                .sourceService("customer-service-command")
                .build();
    }
}
