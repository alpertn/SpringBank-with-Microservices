package com.banking_microservices.money_service_command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateMoneyAccountRequest(
        @NotBlank String userId,
        @NotBlank String keycloakUserUUID
) {
}
