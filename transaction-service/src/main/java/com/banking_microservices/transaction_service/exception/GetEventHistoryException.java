package com.banking_microservices.transaction_service.exception;

public class GetEventHistoryException extends RuntimeException {
    public GetEventHistoryException(String message) {
        super(message);
    }
}
