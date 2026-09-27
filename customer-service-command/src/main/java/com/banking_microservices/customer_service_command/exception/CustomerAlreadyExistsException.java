package com.banking_microservices.customer_service_command.exception;

public class CustomerAlreadyExistsException extends CustomerCommandException {
    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}
