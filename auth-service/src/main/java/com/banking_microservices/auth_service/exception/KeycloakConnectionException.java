package com.banking_microservices.auth_service.exception;

public class KeycloakConnectionException extends RuntimeException {
    public KeycloakConnectionException(String message) {
        super(message);
    }
}
