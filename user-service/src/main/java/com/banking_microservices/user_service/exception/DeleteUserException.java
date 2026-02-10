package com.banking_microservices.user_service.exception;

public class DeleteUserException extends RuntimeException {
    public DeleteUserException(String message) {
        super(message);
    }
}
