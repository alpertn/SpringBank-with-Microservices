package com.banking_microservices.customer_service_query.service;

import com.banking_microservices.customer_service_query.dto.CustomerProjectionEvent;
import com.banking_microservices.customer_service_query.model.CustomerDocument;
import com.banking_microservices.customer_service_query.repository.CustomerMongoRepository;
import com.banking_microservices.customer_service_query.search.CustomerSearchIndexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProjectionServiceTest {

    @Mock
    private CustomerMongoRepository mongoRepository;

    @Mock
    private CustomerSearchIndexer searchIndexer;

    @Test
    void projectIgnoresStaleEvent() {
        CustomerProjectionService service = new CustomerProjectionService(mongoRepository, searchIndexer, () -> "12:00:00");
        CustomerDocument existing = CustomerDocument.builder()
                .id("customer-1")
                .lastSyncedAt(LocalDateTime.parse("2026-01-02T00:00:00"))
                .build();
        when(mongoRepository.findById("customer-1")).thenReturn(Optional.of(existing));

        service.project(event("2026-01-01T00:00:00"));

        verify(mongoRepository, never()).save(any());
        verify(searchIndexer, never()).upsert(any());
    }

    @Test
    void projectUpdatesMongoAndSearchIndex() {
        CustomerProjectionService service = new CustomerProjectionService(mongoRepository, searchIndexer, () -> "12:00:00");
        when(mongoRepository.findById("customer-1")).thenReturn(Optional.empty());

        service.project(event("2026-01-02T00:00:00"));

        verify(mongoRepository).save(any(CustomerDocument.class));
        verify(searchIndexer).upsert(any());
    }

    private CustomerProjectionEvent event(String occurredAt) {
        CustomerProjectionEvent event = new CustomerProjectionEvent();
        event.setEventId("event-1");
        event.setAggregateId("customer-1");
        event.setKeycloakId("keycloak-1");
        event.setRealm("BANKING");
        event.setEmail("test@bank.com");
        event.setUserType("INDIVIDUAL");
        event.setStatus("ACTIVE");
        event.setName("Test");
        event.setSurname("User");
        event.setSex("UNSPECIFIED");
        event.setMfaMethod("NONE");
        event.setPreferredLanguage("tr");
        event.setKycStatus("PENDING");
        event.setNationalityCode("TR");
        event.setOperationType("CUSTOMER_CREATED");
        event.setOccurredAt(occurredAt);
        event.setSourceService("customer-service-command");
        return event;
    }
}
