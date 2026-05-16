package com.banking_microservices.money_service_command.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        LocalDateTime occurredAt,
        String sourceService
) {
}
