package com.banking_microservices.customer_service_command.exception;

public class CustomerNotFoundException extends CustomerCommandException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
