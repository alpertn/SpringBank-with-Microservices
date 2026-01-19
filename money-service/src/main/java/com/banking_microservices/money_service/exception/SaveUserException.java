package com.banking_microservices.money_service.exception;

public class SaveUserException extends RuntimeException {
    public SaveUserException(String message) {
        super(message);
    }
}
