package com.banking_microservices.money_service_query.exception;

public class ReadModelNotFoundException extends MoneyQueryException {

    public ReadModelNotFoundException(String message) {
        super(message);
    }
}
