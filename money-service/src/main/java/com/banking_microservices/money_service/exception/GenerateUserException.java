package com.banking_microservices.money_service.exception;

public class GenerateUserException extends RuntimeException {
    public GenerateUserException(String message) {
        super(message);
    }
}
