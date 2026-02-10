package com.banking_microservices.user_service.exception;

public class UserNotFoundById extends RuntimeException {
    public UserNotFoundById(String message) {
        super(message);
    }
}
