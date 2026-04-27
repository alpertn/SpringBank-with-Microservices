package com.banking_microservices.transaction_service.repository;

import com.banking_microservices.transaction_service.model.SagaEvents;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaEventsRepository extends JpaRepository<SagaEvents , String>{

}
