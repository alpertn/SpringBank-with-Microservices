package com.banking_microservices.admin_service_query.exception;

public class ReadModelNotFoundException extends AdminQueryException {

    public ReadModelNotFoundException(String message) {
        super(message);
    }
}
