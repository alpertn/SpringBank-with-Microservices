package com.banking_microservices.money_service.repository;

import com.banking_microservices.money_service.models.SagaEvents;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaEventsRepository extends JpaRepository<SagaEvents, String> {

    boolean existsByUUID(String uuid);
}
