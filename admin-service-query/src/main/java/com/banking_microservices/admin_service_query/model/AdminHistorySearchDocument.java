package com.banking_microservices.admin_service_query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "admin-query-history", createIndex = false)
public class AdminHistorySearchDocument {

    @Id
    private String requestId;
    private String adminEmail;
    private String transport;
    private String requestType;
    private String targetType;
    private String targetName;
    private String topicName;
    private String status;
    private String responseType;
    private String queryText;
    private String requestPayload;
    private String responsePayload;
    private String errorMessage;
    private LocalDateTime requestedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime lastSyncedAt;
}
