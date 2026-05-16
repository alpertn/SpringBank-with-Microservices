package com.banking_microservices.transaction_service.dto;

import com.banking_microservices.transaction_service.dto.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto {

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "TransactionType is required")
    private TransactionType transactionType;

    // TRANSFER ve WITHDRAW için zorunlu, DEPOSIT için null olabilir
    private String senderIban;

    // TRANSFER için zorunlu, DEPOSIT için nullable, WITHDRAW için null
    private String receiverIban;

    // Sadece TRANSFER için
    private String receiverName;

    // Sadece TRANSFER için
    private String receiverSurname;

    private String description;

    private TokenDetailsDto tokenDetails;
}
