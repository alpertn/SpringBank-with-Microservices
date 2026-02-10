package com.banking_microservices.user_service.exception;

public class UserRoleUpdateException extends RuntimeException {
    public UserRoleUpdateException(String message) {
        super(message);
    }
}
