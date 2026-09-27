package com.banking_microservices.customer_service_command.repository;

import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import com.banking_microservices.customer_service_command.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndDeletedFalse(UUID id);

    Optional<Customer> findByKeycloakIdAndDeletedFalse(UUID keycloakId);

    Optional<Customer> findByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByKeycloakIdAndDeletedFalse(UUID keycloakId);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    List<Customer> findByStatusAndDeletedFalse(UserStatus status);

    List<Customer> findByKycStatusAndDeletedFalse(KycStatus kycStatus);

    List<Customer> findBySpecialCustomerTrueAndDeletedFalse();

    List<Customer> findByRiskScoreGreaterThanEqualAndDeletedFalse(int riskScore);
}
