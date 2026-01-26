package com.banking_microservices.transaction_service.model;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class transaction {

    @Id
    @UuidGenerator
    private String id;

    @Builder.Default
    private String senderUserId = null;

    @Builder.Default
    private String receiverUserId = null;

    @Builder.Default
    private String senderIban = null;

    @Builder.Default
    private String receiverIban = null;

    @Column(precision = 19, scale = 2)
    private BigDecimal money;

    private String transactionType;

    @Builder.Default
    private String description = null;

    @Builder.Default
    private LocalDateTime localDateTime = LocalDateTime.now();

    @Builder.Default
    private Boolean error = false;

    @Builder.Default
    private String errorDescription = null;

    @Builder.Default
    private String status = "PROGRESS";


}
