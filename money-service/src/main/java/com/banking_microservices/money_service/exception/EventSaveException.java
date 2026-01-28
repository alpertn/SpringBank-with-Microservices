package com.banking_microservices.money_service.exception;

public class EventSaveException extends RuntimeException {
    public EventSaveException(String message) {
        super(message);
    }
}
