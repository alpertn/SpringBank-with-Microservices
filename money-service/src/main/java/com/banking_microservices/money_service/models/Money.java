package com.banking_microservices.money_service.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.validator.constraints.UUID;

import java.math.BigDecimal;

@Entity
@Table(name = "money")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Money {

    @Id
    @UuidGenerator
    private String id;

    private String userId;

    private String userIban;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal money = new BigDecimal("0.00");


}
