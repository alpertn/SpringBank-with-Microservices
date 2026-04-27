package com.banking_microservices.money_service.dto;

import com.banking_microservices.money_service.dto.enums.SagaStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "sagavents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEventsDto {


    @Id
    @UuidGenerator
    private String UUID;

    private String kafkaEventUUID;

    private SagaStatus status;

    private String errorDescripton;

    private TransactionEntity transactionEntity;


}

