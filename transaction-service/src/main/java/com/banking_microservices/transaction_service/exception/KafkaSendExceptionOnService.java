package com.banking_microservices.transaction_service.exception;

public class KafkaSendExceptionOnService extends RuntimeException {
    public KafkaSendExceptionOnService(String message) {
        super(message);
    }
}
