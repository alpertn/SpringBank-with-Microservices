package com.banking_microservices.transaction_service.dto.enums;

/**
 * Transaction-service Kafka idempotency kontrolleri icin kullanilan event type sabitleri.
 * Her event icin iki kayit tutulur:
 *  - RECEIVED: mesaj alindi, isleniyor (duplicate'leri bloklar)
 *  - DONE:     islem basariyla tamamlandi
 */
public enum KafkaEventType {
    TX_EFT_RECEIVED,
    TX_EFT_DONE,
    TX_ERROR_RECEIVED,
    TX_ERROR_DONE,
    TX_DEPOSIT_RECEIVED,
    TX_DEPOSIT_DONE,
    TX_WITHDRAW_RECEIVED,
    TX_WITHDRAW_DONE
}
