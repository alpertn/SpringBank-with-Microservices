package com.banking_microservices.transaction_service.dto;

import jakarta.validation.constraints.*; // Spring Boot 3.x (Eskisi javax.validation)
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {


    private String senderIban;

    private String receiverIban;

    private String receiverName;

    private String receiverSurname;

    private BigDecimal amount;

    private String description;

}