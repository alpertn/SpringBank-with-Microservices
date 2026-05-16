package com.banking_microservices.money_service_command.exception;

public class AccountAlreadyExistsException extends MoneyCommandException {

    public AccountAlreadyExistsException(String message) {
        super(message);
    }
}
