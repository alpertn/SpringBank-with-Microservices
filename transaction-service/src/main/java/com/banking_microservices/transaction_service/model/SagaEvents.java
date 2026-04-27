package com.banking_microservices.transaction_service.model;

import com.banking_microservices.transaction_service.dto.TransactionHistory;
import com.banking_microservices.transaction_service.dto.TransactionRequestDto;
import com.banking_microservices.transaction_service.dto.enums.SagaStatus;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
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

    private SagaStatus status;

    private String errorDescripton;

    private TransactionHistory transactionHistory;


}

