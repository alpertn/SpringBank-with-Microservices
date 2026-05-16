package com.banking_microservices.money_service_command.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransferCommandRequest(
        @NotBlank String senderIban,
        @NotBlank String receiverIban,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
