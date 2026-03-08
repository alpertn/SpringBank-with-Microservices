package com.banking_microservices.money_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTransactionTopicMessageDto {

    private String eventUUID;

    @Builder.Default
    private String name = null;

    @Builder.Default
    private String surname = null;

    @Builder.Default
    private String senderUserId = null;

    @Builder.Default
    private String senderIban = null;

    @Builder.Default
    private String receiverUserId = null;

    @Builder.Default
    private String receiverIban = null;

    @JsonProperty("money")
    private BigDecimal money;

    private String transactionType;

    @Builder.Default
    private String description = null;

    @Builder.Default
    private String status = "PROGRESS";

    @Builder.Default
    private Boolean error = false;

    @Builder.Default
    private String errorDescription = null;

    @Builder.Default
    private Boolean isMoneyBlocked = false;

    @Builder.Default
    private Boolean userValidation = false;

    @Builder.Default
    private LocalDateTime localDateTime = LocalDateTime.now();
}