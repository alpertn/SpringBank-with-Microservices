package com.banking_microservices.customer_service_command.exception;

public class InvalidCustomerStateException extends CustomerCommandException {
    public InvalidCustomerStateException(String message) {
        super(message);
    }
}
