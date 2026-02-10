package com.banking_microservices.user_service.exception;

public class UserNotFoundByName extends RuntimeException {
    public UserNotFoundByName(String message) {
        super(message);
    }
}
