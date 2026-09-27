package com.banking_microservices.transaction_service.model;

import com.banking_microservices.transaction_service.dto.SagaTransactionSnapshot;
import com.banking_microservices.transaction_service.dto.enums.SagaStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Data
@Table(name = "sagaevents")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEvents {

    @Id
    @UuidGenerator
    private String UUID;

    private String kafkaEventUUID;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status")
    private SagaStatus status;

    private String errorDescripton;

    @Embedded
    @SerializedName(value = "transaction", alternate = {"transactionHistory", "transactionEntity"})
    @JsonProperty("transaction")
    @JsonAlias({"transactionHistory", "transactionEntity"})
    private SagaTransactionSnapshot transaction;

}


