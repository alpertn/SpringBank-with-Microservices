package com.banking_microservices.money_service.exception;

public class SagaTransactionRollbackException extends RuntimeException {

    public SagaTransactionRollbackException(String message) {
        super(message);
    }

}
