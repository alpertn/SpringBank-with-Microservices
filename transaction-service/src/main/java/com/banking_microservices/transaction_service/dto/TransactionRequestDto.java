package com.banking_microservices.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto {

    @UuidGenerator
    private String eventUUID = UUID.randomUUID().toString();

    @Builder.Default
    private String senderUserId = null;

    @Builder.Default
    private String senderIban = null;

    @Builder.Default
    private String receiverUserId = null;

    @Builder.Default
    private String receiverIban = null;

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
    private LocalDateTime localDateTime = LocalDateTime.now();
}