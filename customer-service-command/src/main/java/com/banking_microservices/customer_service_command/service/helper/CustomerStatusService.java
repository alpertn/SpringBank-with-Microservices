package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.enums.UserStatus;
import com.banking_microservices.customer_service_command.exception.InvalidCustomerStateException;
import com.banking_microservices.customer_service_command.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerStatusService {

    public void updateStatus(Customer customer, UserStatus nextStatus) {
        if (customer.getStatus() == UserStatus.CLOSED && nextStatus != UserStatus.CLOSED) {
            throw new InvalidCustomerStateException("Closed customer can not be reactivated. customerId=" + customer.getId());
        }
        customer.setStatus(nextStatus);
    }
}
