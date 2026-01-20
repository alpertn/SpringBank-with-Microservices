package com.banking_microservices.user_service.exception;

public class UserSaveDatabaseException extends RuntimeException {
    public UserSaveDatabaseException(String message) {
        super(message);
    }
}
