package com.banking_microservices.money_service_command.dto;

import java.math.BigDecimal;

public record MoneyAccountResponseDto(
        String id,
        String userId,
        String keycloakUserUUID,
        String userIban,
        BigDecimal availableBalance,
        BigDecimal blockedBalance
) {
}
