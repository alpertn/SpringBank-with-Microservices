package com.banking_microservices.transaction_service.exception;

public class TransactionSaveException extends RuntimeException {
    public TransactionSaveException(String message) {
        super(message);
    }
}
