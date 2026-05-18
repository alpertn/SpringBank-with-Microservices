package com.banking_microservices.admin_service_query.service;

import com.banking_microservices.admin_service_query.dto.AdminHistoryProjectionEvent;
import com.banking_microservices.admin_service_query.exception.ProjectionSyncException;
import com.banking_microservices.admin_service_query.model.AdminHistoryDocument;
import com.banking_microservices.admin_service_query.model.AdminHistorySearchDocument;
import com.banking_microservices.admin_service_query.repository.AdminHistoryMongoRepository;
import com.banking_microservices.admin_service_query.search.AdminHistorySearchIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminHistoryProjectionService {

    private final AdminHistoryMongoRepository mongoRepository;
    private final AdminHistorySearchIndexer searchIndexer;
    private final Supplier<String> currentTime;

    public void project(AdminHistoryProjectionEvent event) {
        try {
            validate(event);
            LocalDateTime requestedAt = parse(event.getRequestedAt());
            LocalDateTime receivedAt = parse(event.getReceivedAt());
            LocalDateTime now = LocalDateTime.now();

            AdminHistoryDocument mongo = AdminHistoryDocument.builder()
                    .requestId(event.getRequestId())
                    .adminEmail(event.getAdminEmail())
                    .adminPasswordMasked(event.getAdminPasswordMasked())
                    .transport(event.getTransport())
                    .requestType(event.getRequestType())
                    .targetType(event.getTargetType())
                    .targetName(event.getTargetName())
                    .topicName(event.getTopicName())
                    .status(event.getStatus())
                    .responseReceived(event.isResponseReceived())
                    .responseType(event.getResponseType())
                    .queryText(event.getQueryText())
                    .requestPayload(event.getRequestPayload())
                    .responsePayload(event.getResponsePayload())
                    .errorMessage(event.getErrorMessage())
                    .requestedAt(requestedAt)
                    .receivedAt(receivedAt)
                    .lastSyncedAt(now)
                    .build();

            AdminHistorySearchDocument search = AdminHistorySearchDocument.builder()
                    .requestId(event.getRequestId())
                    .adminEmail(event.getAdminEmail())
                    .transport(event.getTransport())
                    .requestType(event.getRequestType())
                    .targetType(event.getTargetType())
                    .targetName(event.getTargetName())
                    .topicName(event.getTopicName())
                    .status(event.getStatus())
                    .responseType(event.getResponseType())
                    .queryText(event.getQueryText())
                    .requestPayload(event.getRequestPayload())
                    .responsePayload(event.getResponsePayload())
                    .errorMessage(event.getErrorMessage())
                    .requestedAt(requestedAt)
                    .receivedAt(receivedAt)
                    .lastSyncedAt(now)
                    .build();

            mongoRepository.save(mongo);
            searchIndexer.upsert(search);
            log.info("({}) Admin history projection synced. requestId={}, status={}",
                    currentTime.get(), event.getRequestId(), event.getStatus());
        } catch (Exception exception) {
            throw new ProjectionSyncException("Admin history projection sync failed for requestId=" + event.getRequestId(), exception);
        }
    }

    private void validate(AdminHistoryProjectionEvent event) {
        if (event == null || event.getRequestId() == null || event.getRequestId().isBlank()) {
            throw new ProjectionSyncException("Admin history requestId is missing", null);
        }
    }

    private LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }
}
