package com.banking_microservices.money_service_command.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MoneyProjectionEvent(
        String eventId,
        String aggregateId,
        String userId,
        String keycloakUserUUID,
        String userIban,
        BigDecimal availableBalance,
        BigDecimal blockedBalance,
        String operationType,
        String occurredAt,
        String sourceService
) {
}
