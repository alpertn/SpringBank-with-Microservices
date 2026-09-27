package com.banking_microservices.user_service.exception;

public class CustomerRollbackException extends RuntimeException {

    public CustomerRollbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
