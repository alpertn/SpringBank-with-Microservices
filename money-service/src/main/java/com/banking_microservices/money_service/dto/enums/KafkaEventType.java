package com.banking_microservices.money_service.dto.enums;

/**
 * Kafka idempotency kontrolleri icin kullanilan event type sabitleri.
 * Butun degerler yalnizca money-service icinde kullanilir.
 */
public enum KafkaEventType {
    EFT_PROCESS,
    DEPOSIT_PROCESS,
    WITHDRAW_PROCESS,
    BLOCK_MONEY,
    FRAUD_CHECKED_EFT,
    USERNAME_VALIDATION,
    TRANSACTION_TOPIC_SERVICE
}
