package com.banking_microservices.money_service.dto;

import com.banking_microservices.money_service.dto.enums.SagaStatus;
import com.google.gson.annotations.SerializedName;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status")
    private SagaStatus status;

    private String errorDescripton;

    @Embedded
    @SerializedName(value = "transactionEntity", alternate = "transactionHistory")
    private TransactionEntity transactionEntity;

}


