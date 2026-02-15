package com.banking_microservices.auth_service.exception;

public class KeycloackUserCreateException extends RuntimeException {
    public KeycloackUserCreateException(String message) {
        super(message);
    }
}
