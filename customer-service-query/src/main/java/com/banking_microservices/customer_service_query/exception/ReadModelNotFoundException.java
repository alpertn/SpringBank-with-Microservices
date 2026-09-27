package com.banking_microservices.customer_service_query.exception;

public class ReadModelNotFoundException extends CustomerQueryException {

    public ReadModelNotFoundException(String message) {
        super(message);
    }
}
