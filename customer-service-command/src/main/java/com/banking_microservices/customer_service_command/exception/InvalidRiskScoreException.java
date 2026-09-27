package com.banking_microservices.customer_service_command.exception;

public class InvalidRiskScoreException extends CustomerCommandException {
    public InvalidRiskScoreException(String message) {
        super(message);
    }
}
