package com.banking_microservices.customer_service_query.service;

import com.banking_microservices.customer_service_query.dto.CustomerReadDto;
import com.banking_microservices.customer_service_query.exception.ReadModelNotFoundException;
import com.banking_microservices.customer_service_query.model.CustomerDocument;
import com.banking_microservices.customer_service_query.repository.CustomerMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerQueryService {

    private final CustomerMongoRepository mongoRepository;

    public CustomerReadDto getById(String id) {
        return mongoRepository.findById(id)
                .filter(customer -> !customer.isDeleted())
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Customer read model not found for id=" + id));
    }

    public CustomerReadDto getByKeycloakId(String keycloakId) {
        return mongoRepository.findByKeycloakIdAndDeletedFalse(keycloakId)
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Customer read model not found for keycloakId=" + keycloakId));
    }

    public CustomerReadDto getByEmail(String email) {
        return mongoRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Customer read model not found for email=" + email));
    }

    public CustomerReadDto getByPhoneNumber(String phoneNumber) {
        return mongoRepository.findByPhoneNumberAndDeletedFalse(phoneNumber)
                .map(this::toDto)
                .orElseThrow(() -> new ReadModelNotFoundException("Customer read model not found for phoneNumber=" + phoneNumber));
    }

    public List<CustomerReadDto> search(String keyword, int limit) {
        return limit(mongoRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCaseOrSurnameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCaseOrKeycloakIdContainingIgnoreCase(
                        keyword, keyword, keyword, keyword, keyword
                ), limit)
                .stream()
                .filter(customer -> !customer.isDeleted())
                .map(this::toDto)
                .toList();
    }

    public List<CustomerReadDto> listByStatus(String status, int limit) {
        return limit(mongoRepository.findByStatusAndDeletedFalse(status), limit).stream().map(this::toDto).toList();
    }

    public List<CustomerReadDto> listByKycStatus(String kycStatus, int limit) {
        return limit(mongoRepository.findByKycStatusAndDeletedFalse(kycStatus), limit).stream().map(this::toDto).toList();
    }

    public List<CustomerReadDto> listSpecialCustomers(int limit) {
        return limit(mongoRepository.findBySpecialCustomerTrueAndDeletedFalse(), limit).stream().map(this::toDto).toList();
    }

    public List<CustomerReadDto> listHighRiskCustomers(int minRiskScore, int limit) {
        return limit(mongoRepository.findByRiskScoreGreaterThanEqualAndDeletedFalse(minRiskScore), limit).stream().map(this::toDto).toList();
    }

    private List<CustomerDocument> limit(List<CustomerDocument> documents, int limit) {
        int normalizedLimit = limit <= 0 ? 40 : Math.min(limit, 200);
        return documents.stream().limit(normalizedLimit).toList();
    }

    private CustomerReadDto toDto(CustomerDocument document) {
        return new CustomerReadDto(
                document.getId(),
                document.getKeycloakId(),
                document.getRealm(),
                document.getEmail(),
                document.isEmailVerified(),
                document.getPhoneNumber(),
                document.isPhoneVerified(),
                document.getUserType(),
                document.getStatus(),
                document.getBirthdate(),
                document.getName(),
                document.getMiddleName(),
                document.getSurname(),
                document.getSex(),
                document.getRiskScore(),
                document.isMfaEnabled(),
                document.getMfaMethod(),
                document.getPreferredLanguage(),
                document.isSpecialCustomer(),
                document.getSpecialCustomerScore(),
                document.getKycStatus(),
                document.getNationalityCode(),
                document.getNationalIdMasked(),
                document.getAccountStatus(),
                document.getMaritalStatus(),
                document.getRiskClass(),
                document.getSegment(),
                document.getSegmentScore(),
                document.getPoliticalExposureStatus(),
                document.getEmploymentCategory(),
                document.getAddress(),
                document.isDeleted(),
                document.getLastOperationType(),
                document.getLastSyncedAt()
        );
    }
}
