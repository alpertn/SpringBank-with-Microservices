package com.banking_microservices.money_service.exception;

public class KafkaCreateUserException extends RuntimeException {
    public KafkaCreateUserException(String message) {
        super(message);
    }
}
