package com.banking_microservices.money_service_command.repository;

import com.banking_microservices.money_service_command.model.UserMoney;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMoneyRepository extends JpaRepository<UserMoney, String> {

    Optional<UserMoney> findByUserId(String userId);

    Optional<UserMoney> findByUserIban(String userIban);

    boolean existsByUserId(String userId);

    boolean existsByKeycloakUserUUID(String keycloakUserUUID);

    boolean existsByUserIban(String userIban);
}
