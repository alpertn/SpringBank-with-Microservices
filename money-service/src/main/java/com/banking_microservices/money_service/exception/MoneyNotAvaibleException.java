package com.banking_microservices.money_service.exception;

public class MoneyNotAvaibleException extends RuntimeException {
    public MoneyNotAvaibleException(String message) {
        super(message);
    }
}
