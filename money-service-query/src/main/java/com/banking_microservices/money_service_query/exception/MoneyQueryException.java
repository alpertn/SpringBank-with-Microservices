package com.banking_microservices.money_service_query.exception;

public class MoneyQueryException extends RuntimeException {

    public MoneyQueryException(String message) {
        super(message);
    }

    public MoneyQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
