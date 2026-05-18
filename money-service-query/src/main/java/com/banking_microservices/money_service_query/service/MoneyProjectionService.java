package com.banking_microservices.money_service_query.service;

import com.banking_microservices.money_service_query.dto.MoneyProjectionEvent;
import com.banking_microservices.money_service_query.exception.ProjectionSyncException;
import com.banking_microservices.money_service_query.model.MoneyAccountDocument;
import com.banking_microservices.money_service_query.model.MoneyAccountSearchDocument;
import com.banking_microservices.money_service_query.repository.MoneyAccountMongoRepository;
import com.banking_microservices.money_service_query.search.MoneyAccountSearchIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyProjectionService {

    // Read-side projection katmani:
    // Kafka'dan gelen event'i alip hem MongoDB read modelini hem Elasticsearch indexini gunceller.
    private final MoneyAccountMongoRepository mongoRepository;
    private final MoneyAccountSearchIndexer searchIndexer;
    private final Supplier<String> currentTime;

    public void project(MoneyProjectionEvent event) {
        try {
            validateEvent(event);
            LocalDateTime occurredAt = parseOccurredAt(event);

            MoneyAccountDocument existingDocument = mongoRepository.findById(event.getAggregateId()).orElse(null);
            if (isStaleEvent(existingDocument, occurredAt)) {
                log.warn("({}) Ignoring stale projection event. eventId={}, aggregateId={}, occurredAt={}, lastSyncedAt={}",
                        currentTime.get(),
                        event.getEventId(),
                        event.getAggregateId(),
                        occurredAt,
                        existingDocument.getLastSyncedAt());
                return;
            }

            // MongoDB ana read modeldir.
            MoneyAccountDocument mongoDocument = MoneyAccountDocument.builder()
                    .id(event.getAggregateId())
                    .userId(event.getUserId())
                    .keycloakUserUUID(event.getKeycloakUserUUID())
                    .userIban(event.getUserIban())
                    .availableBalance(event.getAvailableBalance())
                    .blockedBalance(event.getBlockedBalance())
                    .lastOperationType(event.getOperationType())
                    .lastSyncedAt(occurredAt)
                    .build();

            // Elasticsearch ise arama ve filtreleme odakli ikinci kopyadir.
            MoneyAccountSearchDocument searchDocument = MoneyAccountSearchDocument.builder()
                    .id(event.getAggregateId())
                    .userId(event.getUserId())
                    .keycloakUserUUID(event.getKeycloakUserUUID())
                    .userIban(event.getUserIban())
                    .availableBalance(event.getAvailableBalance())
                    .blockedBalance(event.getBlockedBalance())
                    .lastOperationType(event.getOperationType())
                    .lastSyncedAt(occurredAt)
                    .build();

            mongoRepository.save(mongoDocument);
            searchIndexer.upsert(searchDocument);

            log.info("({}) Projection synced to MongoDB and Elasticsearch. eventId={}, aggregateId={}",
                    currentTime.get(), event.getEventId(), event.getAggregateId());
        } catch (Exception exception) {
            throw new ProjectionSyncException(
                    "Projection sync failed for eventId=" + event.getEventId(),
                    exception
            );
        }
    }

    private void validateEvent(MoneyProjectionEvent event) {
        if (event == null || event.getAggregateId() == null || event.getAggregateId().isBlank()) {
            throw new ProjectionSyncException("Projection event aggregateId is missing", null);
        }
        if (event.getOccurredAt() == null || event.getOccurredAt().isBlank()) {
            throw new ProjectionSyncException(
                    "Projection event occurredAt is missing for aggregateId=" + event.getAggregateId(),
                    null
            );
        }
    }

    private LocalDateTime parseOccurredAt(MoneyProjectionEvent event) {
        try {
            return LocalDateTime.parse(event.getOccurredAt());
        } catch (Exception exception) {
            throw new ProjectionSyncException(
                    "Projection event occurredAt is invalid for aggregateId=" + event.getAggregateId(),
                    exception
            );
        }
    }

    private boolean isStaleEvent(MoneyAccountDocument existingDocument, LocalDateTime occurredAt) {
        return existingDocument != null
                && existingDocument.getLastSyncedAt() != null
                && existingDocument.getLastSyncedAt().isAfter(occurredAt);
    }
}
