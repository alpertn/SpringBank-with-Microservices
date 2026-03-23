package com.banking_microservices.fraud_service.dto.enums;

/**
 * Fraud-service Kafka idempotency kontrolleri icin kullanilan event type sabitleri.
 * Her event icin iki kayit tutulur:
 *  - RECEIVED: mesaj alindi, isleniyor (duplicate'leri bloklar)
 *  - DONE:     islem basariyla tamamlandi
 */
public enum KafkaEventType {
    EFT_CHECK_RECEIVED,
    EFT_CHECK_DONE,
    DEPOSIT_CHECK_RECEIVED,
    DEPOSIT_CHECK_DONE,
    WITHDRAW_CHECK_RECEIVED,
    WITHDRAW_CHECK_DONE
}
