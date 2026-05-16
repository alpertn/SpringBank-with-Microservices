package com.banking_microservices.money_service_query.dto;

import java.math.BigDecimal;

public record MoneyAccountReadDto(
        String id,
        String userId,
        String keycloakUserUUID,
        String userIban,
        BigDecimal availableBalance,
        BigDecimal blockedBalance
) {
}
