package com.banking_microservices.money_service.exception;

public class EventUUIDNotFoundException extends RuntimeException {
    public EventUUIDNotFoundException(String message) {
        super(message);
    }
}
