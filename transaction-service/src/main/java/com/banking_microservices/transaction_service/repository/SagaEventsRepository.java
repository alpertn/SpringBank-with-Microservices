package com.banking_microservices.transaction_service.repository;

import com.banking_microservices.transaction_service.model.SagaEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaEventsRepository extends JpaRepository<SagaEvents, String> {

    Optional<SagaEvents> findByKafkaEventUUID(String kafkaEventUUID);

    List<SagaEvents> findAllByOrderByUUIDAsc();

    boolean existsByKafkaEventUUID(String kafkaEventUUID);

}
