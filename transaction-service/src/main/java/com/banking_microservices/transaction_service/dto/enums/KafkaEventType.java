package com.banking_microservices.transaction_service.dto.enums;

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
