package com.banking_microservices.admin_service_command.service;

import com.banking_microservices.admin_service_command.dto.AdminHistoryCommandRequest;
import com.banking_microservices.admin_service_command.dto.AdminHistoryCommandResponse;
import com.banking_microservices.admin_service_command.dto.AdminHistoryProjectionEvent;
import com.banking_microservices.admin_service_command.kafka.AdminHistoryProjectionPublisher;
import com.banking_microservices.admin_service_command.model.AdminHistoryRecord;
import com.banking_microservices.admin_service_command.repository.AdminHistoryRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminHistoryCommandService {

    private final AdminHistoryRecordRepository repository;
    private final AdminHistoryProjectionPublisher projectionPublisher;
    private final Supplier<String> currentTime;

    @Transactional
    public AdminHistoryCommandResponse upsert(AdminHistoryCommandRequest request) {
        AdminHistoryRecord entity = repository.findById(request.requestId())
                .orElse(AdminHistoryRecord.builder().requestId(request.requestId()).build());

        entity.setAdminEmail(pick(request.adminEmail(), entity.getAdminEmail()));
        entity.setAdminPasswordMasked(pick(request.adminPasswordMasked(), entity.getAdminPasswordMasked()));
        entity.setTransport(pick(request.transport(), entity.getTransport()));
        entity.setRequestType(pick(request.requestType(), entity.getRequestType()));
        entity.setTargetType(pick(request.targetType(), entity.getTargetType()));
        entity.setTargetName(pick(request.targetName(), entity.getTargetName()));
        entity.setTopicName(pick(request.topicName(), entity.getTopicName()));
        entity.setStatus(pick(request.status(), entity.getStatus()));
        entity.setResponseReceived(request.responseReceived() || entity.isResponseReceived());
        entity.setResponseType(pick(request.responseType(), entity.getResponseType()));
        entity.setQueryText(pick(request.queryText(), entity.getQueryText()));
        entity.setRequestPayload(pick(request.requestPayload(), entity.getRequestPayload()));
        entity.setResponsePayload(pick(request.responsePayload(), entity.getResponsePayload()));
        entity.setErrorMessage(pick(request.errorMessage(), entity.getErrorMessage()));
        entity.setRequestedAt(parseDate(request.requestedAt(), entity.getRequestedAt()));
        entity.setReceivedAt(parseDate(request.receivedAt(), entity.getReceivedAt()));

        AdminHistoryRecord saved = repository.save(entity);
        projectionPublisher.publish(toProjection(saved));

        log.info("({}) Admin history record upserted. requestId={}, status={}",
                currentTime.get(), saved.getRequestId(), saved.getStatus());

        return new AdminHistoryCommandResponse(
                saved.getRequestId(),
                saved.getStatus(),
                LocalDateTime.now().toString()
        );
    }

    private AdminHistoryProjectionEvent toProjection(AdminHistoryRecord saved) {
        return AdminHistoryProjectionEvent.builder()
                .requestId(saved.getRequestId())
                .adminEmail(saved.getAdminEmail())
                .adminPasswordMasked(saved.getAdminPasswordMasked())
                .transport(saved.getTransport())
                .requestType(saved.getRequestType())
                .targetType(saved.getTargetType())
                .targetName(saved.getTargetName())
                .topicName(saved.getTopicName())
                .status(saved.getStatus())
                .responseReceived(saved.isResponseReceived())
                .responseType(saved.getResponseType())
                .queryText(saved.getQueryText())
                .requestPayload(saved.getRequestPayload())
                .responsePayload(saved.getResponsePayload())
                .errorMessage(saved.getErrorMessage())
                .requestedAt(saved.getRequestedAt() == null ? null : saved.getRequestedAt().toString())
                .receivedAt(saved.getReceivedAt() == null ? null : saved.getReceivedAt().toString())
                .sourceService("admin-service-command")
                .build();
    }

    private String pick(String incoming, String current) {
        return incoming == null || incoming.isBlank() ? current : incoming;
    }

    private LocalDateTime parseDate(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDateTime.parse(value);
    }
}
