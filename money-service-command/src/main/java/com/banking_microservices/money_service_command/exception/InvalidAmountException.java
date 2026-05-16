package com.banking_microservices.money_service_command.exception;

public class InvalidAmountException extends MoneyCommandException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
