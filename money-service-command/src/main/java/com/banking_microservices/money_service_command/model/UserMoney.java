package com.banking_microservices.money_service_command.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "money_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMoney {

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(name = "keycloak_user_uuid", nullable = false, unique = true)
    private String keycloakUserUUID;

    @Column(nullable = false, unique = true)
    private String userIban;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal money = new BigDecimal("0.00");

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal blockedMoney = new BigDecimal("0.00");
}
