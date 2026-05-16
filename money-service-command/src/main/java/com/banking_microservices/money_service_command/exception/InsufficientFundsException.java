package com.banking_microservices.money_service_command.exception;

public class InsufficientFundsException extends MoneyCommandException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
