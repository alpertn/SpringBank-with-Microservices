package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import com.banking_microservices.customer_service_command.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerDeletionService {

    public void softDelete(Customer customer) {
        customer.setDeleted(true);
        customer.setStatus(UserStatus.CLOSED);
    }
}
