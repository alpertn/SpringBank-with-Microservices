package com.banking_microservices.user_service.dto.enums;

public enum KafkaEventType {
    USER_AUTH_CREATE,
    USER_CREATE_SUCCESS,
    USER_TX_VALIDATE,
    USER_TX_DEPOSIT_VALIDATE,
    USER_TX_WITHDRAW_VALIDATE,
    USER_USERNAME_VALIDATE
}
