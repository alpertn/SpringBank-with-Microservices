package com.banking_microservices.transaction_service.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistory {

    private String id;

    private String eventId;

    private String receiverName;

    private String receiverSurname;

    private String senderUserId;

    private String receiverUserId;

    private String senderIban;

    private String receiverIban;

    @Column(precision = 19, scale = 2)
    private BigDecimal money;

    private String transactionType;

    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime localDateTime;

    @Builder.Default
    private Boolean error = false;

    private String errorDescription;

    @Builder.Default
    private Boolean userValidation = false;

    @Builder.Default
    private String status = "PROGRESS";

}
