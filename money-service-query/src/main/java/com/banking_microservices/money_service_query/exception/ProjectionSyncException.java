package com.banking_microservices.money_service_query.exception;

public class ProjectionSyncException extends MoneyQueryException {

    public ProjectionSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
