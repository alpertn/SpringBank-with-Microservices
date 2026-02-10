package com.banking_microservices.user_service.exception;

public class EmailChangeException extends RuntimeException {
    public EmailChangeException(String message) {
        super(message);
    }
}
