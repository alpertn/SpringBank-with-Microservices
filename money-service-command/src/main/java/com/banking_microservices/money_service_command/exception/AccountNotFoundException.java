package com.banking_microservices.money_service_command.exception;

public class AccountNotFoundException extends MoneyCommandException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
