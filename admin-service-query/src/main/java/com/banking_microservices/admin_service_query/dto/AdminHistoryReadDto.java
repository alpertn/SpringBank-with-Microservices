package com.banking_microservices.admin_service_query.dto;

import java.time.LocalDateTime;

public record AdminHistoryReadDto(
        String requestId,
        String adminEmail,
        String adminPasswordMasked,
        String transport,
        String requestType,
        String targetType,
        String targetName,
        String topicName,
        String status,
        boolean responseReceived,
        String responseType,
        String queryText,
        String requestPayload,
        String responsePayload,
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime receivedAt
) {
}
