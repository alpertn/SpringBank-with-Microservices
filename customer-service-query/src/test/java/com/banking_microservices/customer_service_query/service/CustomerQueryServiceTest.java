package com.banking_microservices.customer_service_query.service;

import com.banking_microservices.customer_service_query.exception.ReadModelNotFoundException;
import com.banking_microservices.customer_service_query.model.CustomerDocument;
import com.banking_microservices.customer_service_query.repository.CustomerMongoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerQueryServiceTest {

    @Mock
    private CustomerMongoRepository mongoRepository;

    @Test
    void getByKeycloakIdReturnsReadModel() {
        CustomerQueryService service = new CustomerQueryService(mongoRepository);
        CustomerDocument document = CustomerDocument.builder()
                .id("customer-1")
                .keycloakId("keycloak-1")
                .email("test@bank.com")
                .name("Test")
                .surname("User")
                .deleted(false)
                .build();
        when(mongoRepository.findByKeycloakIdAndDeletedFalse("keycloak-1")).thenReturn(Optional.of(document));

        var result = service.getByKeycloakId("keycloak-1");

        assertThat(result.id()).isEqualTo("customer-1");
        assertThat(result.email()).isEqualTo("test@bank.com");
    }

    @Test
    void getByIdRejectsMissingReadModel() {
        CustomerQueryService service = new CustomerQueryService(mongoRepository);
        when(mongoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing")).isInstanceOf(ReadModelNotFoundException.class);
    }
}
