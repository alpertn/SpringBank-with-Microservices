package com.banking_microservices.transaction_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawDto {
    @NotNull
    private String accountIban;
    @NotNull
    private BigDecimal amount;
    private String description;
}
