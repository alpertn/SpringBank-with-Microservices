package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.enums.MfaMethod;
import com.banking_microservices.customer_service_command.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerMfaService {

    private final CustomerValidator validator;

    public void updateMfa(Customer customer, boolean enabled, MfaMethod method) {
        validator.assertMfa(enabled, method);
        customer.setMfaEnabled(enabled);
        customer.setMfaMethod(method);
    }
}
