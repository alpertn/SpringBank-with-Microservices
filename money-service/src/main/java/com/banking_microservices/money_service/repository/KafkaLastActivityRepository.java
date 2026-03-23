package com.banking_microservices.money_service.repository;

import com.banking_microservices.money_service.models.KafkaLastActivity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface KafkaLastActivityRepository extends JpaRepository<KafkaLastActivity, String> {
    boolean existsByEventUUID(String eventUUID);
    boolean existsByEventUUIDAndEventType(String eventUUID, String eventType);
}
