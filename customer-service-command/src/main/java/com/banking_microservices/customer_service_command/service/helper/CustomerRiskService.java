package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerRiskService {

    private final CustomerValidator validator;

    public void updateRiskScore(Customer customer, int riskScore) {
        validator.assertScore("riskScore", riskScore);
        customer.setRiskScore(riskScore);
    }

    public void updateSpecialCustomer(Customer customer, boolean specialCustomer, int specialCustomerScore) {
        validator.assertScore("specialCustomerScore", specialCustomerScore);
        customer.setSpecialCustomer(specialCustomer);
        customer.setSpecialCustomerScore(specialCustomerScore);
    }
}
