package com.banking_microservices.money_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import jakarta.persistence.Table;

@Entity
@Table(name = "kafka_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEvent {
    @Id
    private String eventId;
    private String eventType;
    private LocalDateTime createdAt;
}
