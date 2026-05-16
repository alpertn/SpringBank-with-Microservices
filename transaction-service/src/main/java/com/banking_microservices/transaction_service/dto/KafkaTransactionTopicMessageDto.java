package com.banking_microservices.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// DUZELTME: @UuidGenerator bir JPA/Hibernate anotasyonu, DTO'da kullanilmaz. Import kaldirildi.
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.dto.enums.TransactionStatus;
import com.banking_microservices.transaction_service.dto.enums.TransactionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTransactionTopicMessageDto {

    private String eventUUID;

    @Builder.Default
    private String keycloakUserUUID = null;

    @Builder.Default
    private String senderName = null;

    @Builder.Default
    private String senderSurname = null;

    @Builder.Default
    private String senderEmail = null;

    @Builder.Default
    private String receiverEmail = null;

    @Builder.Default
    private String receiverName = null;

    @Builder.Default
    private String receiverSurname = null;

    @Builder.Default
    private String senderUserId = null;

    @Builder.Default
    private String senderIban = null;

    @Builder.Default
    private String receiverUserId = null;

    @Builder.Default
    private String receiverIban = null;

    private BigDecimal money;

    @Builder.Default
    private TransactionType transactionType = TransactionType.TRANSFER;

    @Builder.Default
    private String description = null;

    @Builder.Default
    private TransactionStatus status = TransactionStatus.CREATED;

    @Builder.Default
    private String statusDescription = TransactionStatus.CREATED.getDescription();

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

    @Builder.Default
    private List<TransactionEntity> senderTransactionHistory = null;

    @Builder.Default
    private List<TransactionEntity> receiverTransactionHistory = null;

    @Builder.Default
    private TokenDetailsDto tokenDetails = null;
}
