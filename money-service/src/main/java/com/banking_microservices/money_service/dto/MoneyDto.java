package com.banking_microservices.money_service.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyDto {

    private String id;
    private String userId;

    private String userIban;

    @Builder.Default
    @JsonProperty("money")
    private BigDecimal money = new BigDecimal("0.00");

    @Builder.Default
    @JsonProperty("blockedmoney")
    private BigDecimal blockedMoney = new BigDecimal("0.00");

}
