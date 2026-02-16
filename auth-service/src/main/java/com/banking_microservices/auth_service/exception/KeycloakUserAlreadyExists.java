package com.banking_microservices.auth_service.exception;

public class KeycloakUserAlreadyExists extends RuntimeException {
    public KeycloakUserAlreadyExists(String message) {
        super(message);
    }
}
