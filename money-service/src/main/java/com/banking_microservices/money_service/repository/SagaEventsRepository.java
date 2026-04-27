package com.banking_microservices.money_service.repository;

import com.banking_microservices.money_service.dto.SagaEventsDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SagaEventsRepository extends JpaRepository<SagaEventsDto, String> {

    boolean existsByUUID(String uuid);

    boolean existsByKafkaEventUUID(String kafkaEventUUID);

    Optional<SagaEventsDto> findByKafkaEventUUID(String kafkaEventUUID);

}

