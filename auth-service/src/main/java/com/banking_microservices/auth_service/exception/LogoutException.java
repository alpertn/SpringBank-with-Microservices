package com.banking_microservices.auth_service.exception;

public class LogoutException extends RuntimeException {
    public LogoutException(String message) {
        super(message);
    }
}
