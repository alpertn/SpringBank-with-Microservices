package com.banking_microservices.fraud_service.dto;

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
public class TransactionEntity {
    private String id;
    private String eventId;
    private String receiverName;
    private String receiverSurname;
    private String senderUserId;
    private String receiverUserId;
    private String senderIban;
    private String receiverIban;
    @com.fasterxml.jackson.annotation.JsonProperty("money")
    private BigDecimal money;
    private String transactionType;
    private String description;
    private LocalDateTime localDateTime;
    @Builder.Default
    private Boolean error = false;
    private String errorDescription;
    @Builder.Default
    private Boolean userValidation = false;
    @Builder.Default
    private String status = "PROGRESS";
}
