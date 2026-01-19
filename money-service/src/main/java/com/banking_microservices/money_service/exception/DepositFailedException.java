package com.banking_microservices.money_service.exception;

public class DepositFailedException extends RuntimeException {
    public DepositFailedException(String message) {
        super(message);
    }
}
