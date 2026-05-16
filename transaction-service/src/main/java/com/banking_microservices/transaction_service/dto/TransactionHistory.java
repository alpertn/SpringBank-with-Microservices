package com.banking_microservices.transaction_service.dto;

import com.banking_microservices.transaction_service.config.TokenDetailsConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
public class TransactionHistory {

    @Column(name = "th_id")
    private String id;

    @Column(name = "th_event_id")
    private String eventId;

    @Column(name = "th_receiver_name")
    private String receiverName;

    @Column(name = "th_receiver_surname")
    private String receiverSurname;

    @Column(name = "th_sender_user_id")
    private String senderUserId;

    @Column(name = "th_receiver_user_id")
    private String receiverUserId;

    @Column(name = "th_sender_iban")
    private String senderIban;

    @Column(name = "th_receiver_iban")
    private String receiverIban;

    @Column(name = "th_money", precision = 19, scale = 2)
    private BigDecimal money;

    @Column(name = "th_transaction_type")
    private String transactionType;

    @Column(name = "th_description")
    private String description;

    @Column(name = "th_local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "th_error")
    @Builder.Default
    private Boolean error = false;

    @Column(name = "th_error_description")
    private String errorDescription;

    @Column(name = "th_user_validation")
    @Builder.Default
    private Boolean userValidation = false;

    @Column(name = "th_status")
    @Builder.Default
    private String status = "PROGRESS";

    @Convert(converter = TokenDetailsConverter.class)
    @Column(name = "th_token_details", columnDefinition = "TEXT")
    private TokenDetailsDto tokenDetails;

}
