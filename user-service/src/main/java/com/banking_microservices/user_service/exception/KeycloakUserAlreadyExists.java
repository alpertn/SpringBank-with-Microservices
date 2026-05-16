package com.banking_microservices.user_service.exception;

public class KeycloakUserAlreadyExists extends RuntimeException {
    public KeycloakUserAlreadyExists(String message) {
        super(message);
    }
}
