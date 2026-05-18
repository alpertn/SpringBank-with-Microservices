package com.banking_microservices.money_service_query.service;

import com.banking_microservices.money_service_query.dto.MoneyProjectionEvent;
import com.banking_microservices.money_service_query.exception.ProjectionSyncException;
import com.banking_microservices.money_service_query.model.MoneyAccountDocument;
import com.banking_microservices.money_service_query.model.MoneyAccountSearchDocument;
import com.banking_microservices.money_service_query.repository.MoneyAccountMongoRepository;
import com.banking_microservices.money_service_query.search.MoneyAccountSearchIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyProjectionServiceTest {

    @Mock
    private MoneyAccountMongoRepository mongoRepository;

    @Mock
    private MoneyAccountSearchIndexer searchIndexer;

    private MoneyProjectionService moneyProjectionService;

    @BeforeEach
    void setUp() {
        moneyProjectionService = new MoneyProjectionService(mongoRepository, searchIndexer, () -> "12:00:00");
    }

    @Test
    void projectWritesMongoAndSearchReadModels() {
        LocalDateTime occurredAt = LocalDateTime.parse("2026-05-16T12:00:00");
        when(mongoRepository.findById("account-1")).thenReturn(Optional.empty());

        moneyProjectionService.project(event("event-1", "account-1", occurredAt, "TRANSFER_RECEIVED"));

        ArgumentCaptor<MoneyAccountDocument> mongoCaptor = ArgumentCaptor.forClass(MoneyAccountDocument.class);
        ArgumentCaptor<MoneyAccountSearchDocument> searchCaptor = ArgumentCaptor.forClass(MoneyAccountSearchDocument.class);
        verify(mongoRepository).save(mongoCaptor.capture());
        verify(searchIndexer).upsert(searchCaptor.capture());

        assertThat(mongoCaptor.getValue().getAvailableBalance()).isEqualByComparingTo("1000.00");
        assertThat(mongoCaptor.getValue().getLastOperationType()).isEqualTo("TRANSFER_RECEIVED");
        assertThat(searchCaptor.getValue().getUserIban()).isEqualTo("TRRECEIVER");
    }

    @Test
    void projectIgnoresStaleEventSoOlderKafkaMessagesCannotOverwriteNewerBalance() {
        LocalDateTime latest = LocalDateTime.parse("2026-05-16T12:01:00");
        LocalDateTime stale = LocalDateTime.parse("2026-05-16T12:00:00");
        when(mongoRepository.findById("account-1")).thenReturn(Optional.of(MoneyAccountDocument.builder()
                .id("account-1")
                .lastSyncedAt(latest)
                .build()));

        moneyProjectionService.project(event("event-stale", "account-1", stale, "DEPOSIT"));

        verify(mongoRepository, never()).save(org.mockito.Mockito.any());
        verify(searchIndexer, never()).upsert(org.mockito.Mockito.any());
    }

    @Test
    void projectRejectsMissingAggregateId() {
        assertThatThrownBy(() -> moneyProjectionService.project(event("event-1", "", LocalDateTime.now(), "DEPOSIT")))
                .isInstanceOf(ProjectionSyncException.class);
    }

    private MoneyProjectionEvent event(String eventId, String aggregateId, LocalDateTime occurredAt, String operation) {
        return MoneyProjectionEvent.builder()
                .eventId(eventId)
                .aggregateId(aggregateId)
                .userId("receiver-user")
                .keycloakUserUUID("receiver-keycloak")
                .userIban("TRRECEIVER")
                .availableBalance(new BigDecimal("1000.00"))
                .blockedBalance(new BigDecimal("0.00"))
                .operationType(operation)
                .occurredAt(occurredAt.toString())
                .sourceService("money-service-command")
                .build();
    }
}
