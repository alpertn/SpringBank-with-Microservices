package com.banking_microservices.money_service_query.dto;

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
public class MoneyProjectionEvent {

    private String eventId;
    private String aggregateId;
    private String userId;
    private String keycloakUserUUID;
    private String userIban;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private String operationType;
    private LocalDateTime occurredAt;
    private String sourceService;
}
