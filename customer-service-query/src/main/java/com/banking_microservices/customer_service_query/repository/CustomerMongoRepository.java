package com.banking_microservices.customer_service_query.repository;

import com.banking_microservices.customer_service_query.model.CustomerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerMongoRepository extends MongoRepository<CustomerDocument, String> {

    Optional<CustomerDocument> findByKeycloakIdAndDeletedFalse(String keycloakId);

    Optional<CustomerDocument> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<CustomerDocument> findByPhoneNumberAndDeletedFalse(String phoneNumber);

    List<CustomerDocument> findByStatusAndDeletedFalse(String status);

    List<CustomerDocument> findByKycStatusAndDeletedFalse(String kycStatus);

    List<CustomerDocument> findBySpecialCustomerTrueAndDeletedFalse();

    List<CustomerDocument> findByRiskScoreGreaterThanEqualAndDeletedFalse(int riskScore);

    List<CustomerDocument> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCaseOrSurnameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCaseOrKeycloakIdContainingIgnoreCase(
            String email,
            String name,
            String surname,
            String phoneNumber,
            String keycloakId
    );
}
