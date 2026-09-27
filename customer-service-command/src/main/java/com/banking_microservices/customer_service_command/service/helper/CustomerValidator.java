package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.dto.enums.KycStatus;
import com.banking_microservices.customer_service_command.dto.enums.MfaMethod;
import com.banking_microservices.customer_service_command.exception.InvalidCustomerStateException;
import com.banking_microservices.customer_service_command.exception.InvalidKycTransitionException;
import com.banking_microservices.customer_service_command.exception.InvalidRiskScoreException;
import com.banking_microservices.customer_service_command.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerValidator {

    public void assertRange(String field, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new InvalidRiskScoreException(field + " must be between " + minimum + " and " + maximum);
        }
    }

    public void assertWritable(Customer customer) {
        if (customer.isDeleted()) {
            throw new InvalidCustomerStateException("Deleted customer can not be updated. customerId=" + customer.getId());
        }
    }

    public void assertScore(String field, int score) {
        if (score < 0 || score > 100) {
            throw new InvalidRiskScoreException(field + " must be between 0 and 100");
        }
    }

    public void assertMfa(boolean enabled, MfaMethod method) {
        MfaMethod resolved = method == null ? MfaMethod.NONE : method;
        if (enabled && resolved == MfaMethod.NONE) {
            throw new InvalidCustomerStateException("mfaMethod must be selected when mfaEnabled=true");
        }
        if (!enabled && resolved != MfaMethod.NONE) {
            throw new InvalidCustomerStateException("mfaMethod must be NONE when mfaEnabled=false");
        }
    }

    public void assertKycTransition(Customer customer, KycStatus nextStatus) {
        if (customer.getKycStatus() == KycStatus.REJECTED && nextStatus == KycStatus.APPROVED) {
            throw new InvalidKycTransitionException("Rejected KYC can not be directly approved. customerId=" + customer.getId());
        }
    }
}
