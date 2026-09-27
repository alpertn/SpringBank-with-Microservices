package com.banking_microservices.customer_service_query.service;

import com.banking_microservices.customer_service_query.dto.CustomerProjectionEvent;
import com.banking_microservices.customer_service_query.exception.ProjectionSyncException;
import com.banking_microservices.customer_service_query.model.CustomerDocument;
import com.banking_microservices.customer_service_query.model.CustomerSearchDocument;
import com.banking_microservices.customer_service_query.repository.CustomerMongoRepository;
import com.banking_microservices.customer_service_query.search.CustomerSearchIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProjectionService {

    private final CustomerMongoRepository mongoRepository;
    private final CustomerSearchIndexer searchIndexer;
    private final Supplier<String> currentTime;

    public void project(CustomerProjectionEvent event) {
        try {
            validate(event);
            LocalDateTime occurredAt = parseOccurredAt(event);

            CustomerDocument existingDocument = mongoRepository.findById(event.getAggregateId()).orElse(null);
            if (isStaleEvent(existingDocument, occurredAt)) {
                log.warn("({}) Ignoring stale customer projection event. eventId={}, aggregateId={}",
                        currentTime.get(), event.getEventId(), event.getAggregateId());
                return;
            }

            CustomerDocument mongoDocument = toMongoDocument(event, occurredAt);
            CustomerSearchDocument searchDocument = toSearchDocument(event, occurredAt);
            mongoRepository.save(mongoDocument);
            searchIndexer.upsert(searchDocument);
            log.info("({}) Customer projection synced. eventId={}, aggregateId={}, operation={}",
                    currentTime.get(), event.getEventId(), event.getAggregateId(), event.getOperationType());
        } catch (Exception exception) {
            throw new ProjectionSyncException("Customer projection sync failed for eventId=" + event.getEventId(), exception);
        }
    }

    private CustomerDocument toMongoDocument(CustomerProjectionEvent event, LocalDateTime occurredAt) {
        return CustomerDocument.builder()
                .id(event.getAggregateId())
                .keycloakId(event.getKeycloakId())
                .realm(event.getRealm())
                .email(event.getEmail())
                .emailVerified(event.isEmailVerified())
                .phoneNumber(event.getPhoneNumber())
                .phoneVerified(event.isPhoneVerified())
                .userType(event.getUserType())
                .status(event.getStatus())
                .birthdate(parseBirthdate(event))
                .name(event.getName())
                .middleName(event.getMiddleName())
                .surname(event.getSurname())
                .sex(event.getSex())
                .riskScore(event.getRiskScore())
                .mfaEnabled(event.isMfaEnabled())
                .mfaMethod(event.getMfaMethod())
                .preferredLanguage(event.getPreferredLanguage())
                .specialCustomer(event.isSpecialCustomer())
                .specialCustomerScore(event.getSpecialCustomerScore())
                .kycStatus(event.getKycStatus())
                .nationalityCode(event.getNationalityCode())
                .nationalIdMasked(event.getNationalIdMasked())
                .accountStatus(event.getAccountStatus())
                .maritalStatus(event.getMaritalStatus())
                .riskClass(event.getRiskClass())
                .segment(event.getSegment())
                .segmentScore(event.getSegmentScore())
                .politicalExposureStatus(event.getPoliticalExposureStatus())
                .employmentCategory(event.getEmploymentCategory())
                .address(event.getAddress())
                .deleted(event.isDeleted())
                .lastOperationType(event.getOperationType())
                .lastSyncedAt(occurredAt)
                .build();
    }

    private CustomerSearchDocument toSearchDocument(CustomerProjectionEvent event, LocalDateTime occurredAt) {
        return CustomerSearchDocument.builder()
                .id(event.getAggregateId())
                .keycloakId(event.getKeycloakId())
                .realm(event.getRealm())
                .email(event.getEmail())
                .phoneNumber(event.getPhoneNumber())
                .userType(event.getUserType())
                .status(event.getStatus())
                .name(event.getName())
                .middleName(event.getMiddleName())
                .surname(event.getSurname())
                .riskScore(event.getRiskScore())
                .specialCustomer(event.isSpecialCustomer())
                .kycStatus(event.getKycStatus())
                .nationalityCode(event.getNationalityCode())
                .accountStatus(event.getAccountStatus())
                .riskClass(event.getRiskClass())
                .segment(event.getSegment())
                .segmentScore(event.getSegmentScore())
                .politicalExposureStatus(event.getPoliticalExposureStatus())
                .addressText(event.getAddress() == null ? null : event.getAddress().getRawAddress())
                .deleted(event.isDeleted())
                .lastOperationType(event.getOperationType())
                .lastSyncedAt(occurredAt)
                .build();
    }

    private void validate(CustomerProjectionEvent event) {
        if (event == null || event.getAggregateId() == null || event.getAggregateId().isBlank()) {
            throw new ProjectionSyncException("Customer projection aggregateId is missing", null);
        }
        if (event.getOccurredAt() == null || event.getOccurredAt().isBlank()) {
            throw new ProjectionSyncException("Customer projection occurredAt is missing for aggregateId=" + event.getAggregateId(), null);
        }
    }

    private LocalDateTime parseOccurredAt(CustomerProjectionEvent event) {
        try {
            return LocalDateTime.parse(event.getOccurredAt());
        } catch (Exception exception) {
            throw new ProjectionSyncException("Customer projection occurredAt is invalid for aggregateId=" + event.getAggregateId(), exception);
        }
    }

    private LocalDateTime parseBirthdate(CustomerProjectionEvent event) {
        String birthdate = event.getBirthdate();
        if (birthdate == null || birthdate.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(birthdate);
        } catch (Exception exception) {
            throw new ProjectionSyncException("Customer birthdate is invalid for aggregateId=" + event.getAggregateId(), exception);
        }
    }

    private boolean isStaleEvent(CustomerDocument existingDocument, LocalDateTime occurredAt) {
        return existingDocument != null
                && existingDocument.getLastSyncedAt() != null
                && existingDocument.getLastSyncedAt().isAfter(occurredAt);
    }
}
