package com.banking_microservices.customer_service_command.exception;

public class CustomerCommandException extends RuntimeException {
    public CustomerCommandException(String message) {
        super(message);
    }

    public CustomerCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
