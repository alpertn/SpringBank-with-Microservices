package com.banking_microservices.user_service.dto.user;

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

    private String receiverName= null;

    @Builder.Default
    private String senderName = null;

    @Builder.Default
    private String senderSurname = null;

    @Builder.Default
    private String senderEmail = null;

    @Builder.Default
    private String receiverEmail = null;

    private String receiverSurname= null;

    private String senderUserId= null;

    private String receiverUserId= null;

    private String senderIban= null;

    private String receiverIban= null;

    @com.fasterxml.jackson.annotation.JsonProperty("money")
    private BigDecimal money;

    private String transactionType;

    private String description = null;

    private LocalDateTime localDateTime;

    @Builder.Default
    private Boolean error = false;

    private String errorDescription;

    @Builder.Default
    private Boolean userValidation = false;

    @Builder.Default
    private Boolean isMoneyBlocked = false;

    @Builder.Default
    private String status = "PROGRESS";

    private TokenDetailsDto tokenDetails;
}
