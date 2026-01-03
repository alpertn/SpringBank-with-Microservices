package com.springbank.money_service.exception;

public class IbanNotFoundException extends RuntimeException {
    public IbanNotFoundException(String message) {
        super(message);
    }
}
