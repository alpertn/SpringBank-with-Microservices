package com.banking_microservices.user_service.exception;

public class KeycloakConnectionException extends RuntimeException {
    public KeycloakConnectionException(String message) {
        super(message);
    }
}
