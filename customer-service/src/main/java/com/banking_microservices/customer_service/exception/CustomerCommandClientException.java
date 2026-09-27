package com.banking_microservices.customer_service.exception;

public class CustomerCommandClientException extends CustomerServiceException {

    public CustomerCommandClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
