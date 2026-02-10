package com.banking_microservices.transaction_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    @UuidGenerator
    private String id;

    private String eventId;

    private String receiverName;

    private String receiverSurname;

    private String senderUserId;

    private String receiverUserId;

    private String senderIban;

    private String receiverIban;

    @Column(precision = 19, scale = 2)
    private BigDecimal money;

    private String transactionType;

    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime localDateTime;

    @Builder.Default
    private Boolean error = false;

    private String errorDescription;

    @Builder.Default
    private String status = "PROGRESS";

    @PrePersist
    protected void onCreate() {
        if (localDateTime == null) {
            localDateTime = LocalDateTime.now();
        }
    }

}
