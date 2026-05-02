package com.banking_microservices.money_service.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Column(name = "te_id")
    private String id;

    @Column(name = "te_event_id")
    private String eventId;

    @Column(name = "te_receiver_name")
    @Builder.Default
    private String receiverName = null;

    @Column(name = "te_sender_name")
    @Builder.Default
    private String senderName = null;

    @Column(name = "te_sender_surname")
    @Builder.Default
    private String senderSurname = null;

    @Column(name = "te_sender_email")
    @Builder.Default
    private String senderEmail = null;

    @Column(name = "te_receiver_email")
    @Builder.Default
    private String receiverEmail = null;

    @Column(name = "te_receiver_surname")
    @Builder.Default
    private String receiverSurname = null;

    @Column(name = "te_sender_user_id")
    @Builder.Default
    private String senderUserId = null;

    @Column(name = "te_receiver_user_id")
    @Builder.Default
    private String receiverUserId = null;

    @Column(name = "te_sender_iban")
    @Builder.Default
    private String senderIban = null;

    @Column(name = "te_receiver_iban")
    @Builder.Default
    private String receiverIban = null;

    @com.fasterxml.jackson.annotation.JsonProperty("money")
    @Column(name = "te_money", precision = 19, scale = 2)
    private BigDecimal money;

    @Column(name = "te_transaction_type")
    private String transactionType;

    @Column(name = "te_description")
    @Builder.Default
    private String description = null;

    @Column(name = "te_local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "te_error")
    @Builder.Default
    private Boolean error = false;

    @Column(name = "te_error_description")
    private String errorDescription;

    @Column(name = "te_user_validation")
    @Builder.Default
    private Boolean userValidation = false;

    @Column(name = "transaction_status")
    @Builder.Default
    private String status = "PROGRESS";

}

