package com.banking_microservices.admin_service_command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHistoryProjectionEvent {
    private String requestId;
    private String adminEmail;
    private String adminPasswordMasked;
    private String transport;
    private String requestType;
    private String targetType;
    private String targetName;
    private String topicName;
    private String status;
    private boolean responseReceived;
    private String responseType;
    private String queryText;
    private String requestPayload;
    private String responsePayload;
    private String errorMessage;
    private String requestedAt;
    private String receivedAt;
    private String sourceService;
}
