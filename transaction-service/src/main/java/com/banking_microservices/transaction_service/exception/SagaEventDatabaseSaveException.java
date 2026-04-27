package com.banking_microservices.transaction_service.exception;

public class SagaEventDatabaseSaveException extends RuntimeException {
    public SagaEventDatabaseSaveException(String message) {
        super(message);
    }
}
