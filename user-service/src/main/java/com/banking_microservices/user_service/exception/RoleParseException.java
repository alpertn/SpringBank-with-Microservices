package com.banking_microservices.user_service.exception;

public class RoleParseException extends RuntimeException {
    public RoleParseException(String message) {
        super(message);
    }
}
