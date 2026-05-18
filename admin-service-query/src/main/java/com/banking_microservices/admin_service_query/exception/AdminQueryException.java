package com.banking_microservices.admin_service_query.exception;

public class AdminQueryException extends RuntimeException {

    public AdminQueryException(String message) {
        super(message);
    }

    public AdminQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
