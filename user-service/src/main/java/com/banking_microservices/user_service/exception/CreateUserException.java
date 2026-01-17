package com.banking_microservices.user_service.exception;

public class CreateUserException extends RuntimeException {
    public CreateUserException(String message) {
        super(message);
    }
}
