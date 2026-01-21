package com.banking_microservices.transaction_service.exception;

public class GetErrorLogsException extends RuntimeException {
    public GetErrorLogsException(String message) {
        super(message);
    }
}
