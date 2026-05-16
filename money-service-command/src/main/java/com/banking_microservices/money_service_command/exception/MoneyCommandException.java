package com.banking_microservices.money_service_command.exception;

public class MoneyCommandException extends RuntimeException {

    public MoneyCommandException(String message) {
        super(message);
    }

    public MoneyCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
