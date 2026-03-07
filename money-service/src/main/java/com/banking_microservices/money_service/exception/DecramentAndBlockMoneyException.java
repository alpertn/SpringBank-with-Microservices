package com.banking_microservices.money_service.exception;

public class DecramentAndBlockMoneyException extends RuntimeException {
    public DecramentAndBlockMoneyException(String message) {
        super(message);
    }
}
