package com.banking_microservices.money_service.exception;

public class KafkaSendException extends RuntimeException {
    public KafkaSendException(String message) {
        super(message);
    }
}
