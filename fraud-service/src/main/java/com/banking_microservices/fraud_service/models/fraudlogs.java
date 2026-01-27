package com.banking_microservices.fraud_service.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "fraud-logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class fraudlogs {
    @Id
    @UuidGenerator
    private String id;
}

