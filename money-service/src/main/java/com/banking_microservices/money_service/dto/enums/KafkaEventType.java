package com.banking_microservices.money_service.dto.enums;

public enum KafkaEventType {
    EFT_PROCESS,
    TRANSACTION_PROCESS,
    BLOCK_MONEY,
    FRAUD_CHECKED_EFT,
    TRANSFER_CREATED,
    USER_CREATE,
    TRANSACTION_TOPIC_SERVICE,
    TRANSFER_EXECUTE
}
