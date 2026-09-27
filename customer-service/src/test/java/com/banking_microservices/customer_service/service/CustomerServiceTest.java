package com.banking_microservices.customer_service.service;

import com.banking_microservices.customer_service.dto.CreateCustomerRequest;
import com.banking_microservices.customer_service.dto.CustomerReadDto;
import com.banking_microservices.customer_service.grpc.CustomerCommandGrpcClient;
import com.banking_microservices.customer_service.grpc.CustomerQueryGrpcClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    @Test
    void createCustomerDelegatesToCommandClient() {
        CustomerCommandGrpcClient commandClient = mock(CustomerCommandGrpcClient.class);
        CustomerQueryGrpcClient queryClient = mock(CustomerQueryGrpcClient.class);
        CustomerService service = new CustomerService(commandClient, queryClient, () -> "12:00:00");
        CreateCustomerRequest request = new CreateCustomerRequest("keycloak-1", "test@bank.com", true, "", false, "", "Test", "", "User");
        CustomerReadDto expected = new CustomerReadDto("customer-1", "keycloak-1", "BANKING", "test@bank.com", true, "", false,
                "INDIVIDUAL", "ACTIVE", "", "Test", "", "User", "UNSPECIFIED", 0, false, "NONE", "tr",
                false, 0, "PENDING", "TR", false, "", "");
        when(commandClient.createCustomer(request)).thenReturn(expected);

        CustomerReadDto result = service.createCustomer(request);

        assertThat(result).isEqualTo(expected);
        verify(commandClient).createCustomer(request);
    }
}
