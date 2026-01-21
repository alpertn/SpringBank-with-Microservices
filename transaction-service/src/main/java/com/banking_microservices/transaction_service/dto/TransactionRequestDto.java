package com.banking_microservices.transaction_service.dto;

import com.banking_microservices.transaction_service.model.TransactionType;
import com.banking_microservices.transaction_service.model.status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.lang.model.type.ErrorType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto {
    @Builder.Default
    private String senderUserId = null;

    @Builder.Default
    private String senderIban = null;

    @Builder.Default
    private String receiverUserId = null;

    @Builder.Default
    private String receiverIban = null;

    private BigDecimal money;

    private TransactionType type;

    @Builder.Default
    private String description = null;

    @Builder.Default
    private BigDecimal senderBalanceAfterTransaction = null;

    @Builder.Default
    private BigDecimal receiverBalanceAfterTransaction = null;

    @Builder.Default
    private status status = com.banking_microservices.transaction_service.model.status.PROGRESS;

    @Builder.Default
    private Boolean error = false;

    @Builder.Default
    private String errorDescription = null;

    @Builder.Default
    private LocalDateTime localDateTime = LocalDateTime.now();
}