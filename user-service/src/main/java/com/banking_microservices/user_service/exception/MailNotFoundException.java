package com.banking_microservices.user_service.exception;

public class MailNotFoundException extends RuntimeException {
    public MailNotFoundException(String message) {
        super(message);
    }
}
