package com.banking_microservices.customer_service_query.exception;

public class CustomerQueryException extends RuntimeException {

    public CustomerQueryException(String message) {
        super(message);
    }

    public CustomerQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
