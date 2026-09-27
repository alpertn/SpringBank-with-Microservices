package com.banking_microservices.customer_service_command.exception;

public class InvalidKycTransitionException extends CustomerCommandException {
    public InvalidKycTransitionException(String message) {
        super(message);
    }
}
