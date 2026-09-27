package com.banking_microservices.customer_service.exception;

public class CustomerAccessDeniedException extends RuntimeException {

    public CustomerAccessDeniedException(String message) {
        super(message);
    }
}
