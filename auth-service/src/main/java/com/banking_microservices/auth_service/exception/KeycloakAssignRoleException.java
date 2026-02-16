package com.banking_microservices.auth_service.exception;

public class KeycloakAssignRoleException extends RuntimeException {
    public KeycloakAssignRoleException(String message) {
        super(message);
    }
}
