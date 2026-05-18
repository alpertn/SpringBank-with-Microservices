package com.banking_microservices.admin_service_command.dto;

import lombok.Builder;

@Builder
public record AdminHistoryCommandRequest(
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
        String requestedAt,
        String receivedAt
) {
}
