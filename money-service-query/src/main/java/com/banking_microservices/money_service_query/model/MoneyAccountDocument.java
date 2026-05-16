package com.banking_microservices.money_service_query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "money_accounts")
public class MoneyAccountDocument {

    @Id
    private String id;
    private String userId;
    private String keycloakUserUUID;
    private String userIban;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private String lastOperationType;
    private LocalDateTime lastSyncedAt;
}
