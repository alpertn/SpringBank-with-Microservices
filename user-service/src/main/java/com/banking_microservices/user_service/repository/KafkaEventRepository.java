package com.banking_microservices.user_service.repository;

import com.banking_microservices.user_service.models.KafkaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KafkaEventRepository extends JpaRepository<KafkaEvent, String> {
    boolean existsByEventIdAndEventType(String eventId, String eventType);
}
