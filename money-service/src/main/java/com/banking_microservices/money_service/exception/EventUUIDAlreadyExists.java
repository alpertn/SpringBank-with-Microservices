package com.banking_microservices.money_service.exception;

public class EventUUIDAlreadyExists extends RuntimeException {
    public EventUUIDAlreadyExists(String message) {
        super(message);
    }
}
