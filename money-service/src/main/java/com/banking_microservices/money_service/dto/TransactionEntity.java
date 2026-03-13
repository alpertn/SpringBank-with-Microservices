package com.banking_microservices.money_service.dto;

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

    private String receiverName= null;

    @Builder.Default
    private String senderName = null;

    @Builder.Default
    private String senderSurname = null;

    @Builder.Default
    private String senderEmail = null;

    @Builder.Default
    private String receiverEmail = null;

    private String receiverSurname= null;

    private String senderUserId= null;

    private String receiverUserId= null;

    private String senderIban= null;

    private String receiverIban= null;

    @Column(precision = 19, scale = 2)
    @com.fasterxml.jackson.annotation.JsonProperty("money")
    private BigDecimal money;

    private String transactionType;

    private String description = null;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime localDateTime;

    @Builder.Default
    private Boolean error = false;

    private String errorDescription;

    @Builder.Default
    private Boolean userValidation = false;

    @Builder.Default
    private String status = "PROGRESS";

    @PrePersist
    protected void onCreate() {
        if (localDateTime == null) {
            localDateTime = LocalDateTime.now();
        }
    }

}
