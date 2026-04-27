package com.banking_microservices.transaction_service.exception;

public class SagaEventNotFoundException extends RuntimeException {
    public SagaEventNotFoundException(String message) {
        super(message);
    }
}
