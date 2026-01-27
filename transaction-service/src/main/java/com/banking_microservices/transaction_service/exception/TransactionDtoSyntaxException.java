package com.banking_microservices.transaction_service.exception;

public class TransactionDtoSyntaxException extends RuntimeException {
    public TransactionDtoSyntaxException(String message) {
        super(message);
    }
}
