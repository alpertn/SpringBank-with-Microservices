package com.banking_microservices.user_service.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kafka_event",
       uniqueConstraints = @UniqueConstraint(columnNames = {"eventId", "eventType"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String eventId;
    private String eventType;
    private LocalDateTime createdAt;
}
