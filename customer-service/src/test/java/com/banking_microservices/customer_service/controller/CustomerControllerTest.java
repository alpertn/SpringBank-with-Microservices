package com.banking_microservices.customer_service.controller;

import com.banking_microservices.customer_service.dto.CustomerProfileUpdateRequest;
import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.exception.CustomerAccessDeniedException;
import com.banking_microservices.customer_service.service.CustomerService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerControllerTest {

    @Test
    void updateMyProfileRejectsAnotherCustomersId() {
        CustomerService customerService = mock(CustomerService.class);
        CustomerController controller = new CustomerController(customerService);
        CustomerProfileUpdateRequest request = profileRequest("customer-2");
        when(customerService.getByKeycloakId("keycloak-owner")).thenReturn(customer("customer-1"));

        assertThatThrownBy(() -> controller.updateMyProfile("keycloak-owner", request))
                .isInstanceOf(CustomerAccessDeniedException.class);

        verify(customerService, never()).updateProfile(request);
    }

    @Test
    void updateMyProfileAllowsCurrentCustomer() {
        CustomerService customerService = mock(CustomerService.class);
        CustomerController controller = new CustomerController(customerService);
        CustomerProfileUpdateRequest request = profileRequest("customer-1");
        when(customerService.getByKeycloakId("keycloak-owner")).thenReturn(customer("customer-1"));

        controller.updateMyProfile("keycloak-owner", request);

        verify(customerService).updateProfile(request);
    }

    private CustomerProfileUpdateRequest profileRequest(String customerId) {
        return new CustomerProfileUpdateRequest(customerId, "customer@bank.com", null, null,
                "Customer", null, "Bank", "UNSPECIFIED", "tr", "TR");
    }

    private CustomerReadDto customer(String customerId) {
        return new CustomerReadDto(customerId, "keycloak-owner", "BANKING", "customer@bank.com", true,
                null, false, "INDIVIDUAL", "ACTIVE", "", "Customer", null, "Bank", "UNSPECIFIED",
                0, false, "NONE", "tr", false, 0, "PENDING", "TR", false, "", "");
    }
}
