package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import com.banking_microservices.customer_service_command.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerKycService {

    private final CustomerValidator validator;

    public void updateKycStatus(Customer customer, KycStatus nextStatus) {
        validator.assertKycTransition(customer, nextStatus);
        customer.setKycStatus(nextStatus);
    }
}
