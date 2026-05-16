package com.banking_microservices.user_service.exception;

public class LoginException extends RuntimeException {
    public LoginException(String message) {
        super(message);
    }
}
