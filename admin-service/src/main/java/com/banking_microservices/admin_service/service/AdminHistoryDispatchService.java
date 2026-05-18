package com.banking_microservices.admin_service.service;

import com.banking_microservices.admin_service.dto.AdminHistoryCommandRequest;
import com.banking_microservices.admin_service.grpc.AdminCommandGrpcClient;
import com.banking_microservices.admin_service.kafka.AdminHistoryKafkaPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class AdminHistoryDispatchService {

    private final AdminCommandGrpcClient grpcClient;
    private final AdminHistoryKafkaPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public String newRequestId() {
        return "admin-" + UUID.randomUUID();
    }

    public void recordSync(String requestId, Map<String, String> adminUser, String requestType, String targetType,
                           String targetName, String topicName, String queryText, Object requestPayload,
                           Object responsePayload, String responseType, String status, String errorMessage) {
        grpcClient.upsert(buildRequest(requestId, adminUser, "grpc", requestType, targetType, targetName, topicName,
                queryText, requestPayload, responsePayload, responseType, status, errorMessage, true, LocalDateTime.now(), LocalDateTime.now()));
    }

    public void recordKafkaPending(String requestId, Map<String, String> adminUser, String requestType, String targetType,
                                   String targetName, String topicName, String queryText, Object requestPayload) {
        kafkaPublisher.publish(buildRequest(requestId, adminUser, "kafka", requestType, targetType, targetName, topicName,
                queryText, requestPayload, null, "async-accepted", "PENDING", "", false, LocalDateTime.now(), null));
    }

    public void runAsyncCompletion(Runnable task) {
        executor.submit(task);
    }

    public void recordKafkaCompletion(String requestId, Map<String, String> adminUser, String requestType, String targetType,
                                      String targetName, String topicName, String queryText, Object requestPayload,
                                      Object responsePayload, String responseType, String status, String errorMessage) {
        kafkaPublisher.publish(buildRequest(requestId, adminUser, "kafka", requestType, targetType, targetName, topicName,
                queryText, requestPayload, responsePayload, responseType, status, errorMessage, true, LocalDateTime.now(), LocalDateTime.now()));
    }

    private AdminHistoryCommandRequest buildRequest(String requestId, Map<String, String> adminUser, String transport,
                                                    String requestType, String targetType, String targetName, String topicName,
                                                    String queryText, Object requestPayload, Object responsePayload,
                                                    String responseType, String status, String errorMessage,
                                                    boolean responseReceived, LocalDateTime requestedAt, LocalDateTime receivedAt) {
        return AdminHistoryCommandRequest.builder()
                .requestId(requestId)
                .adminEmail(adminUser.getOrDefault("adminEmail", "unknown-admin"))
                .adminPasswordMasked(adminUser.getOrDefault("adminPasswordMasked", "NOT_STORED"))
                .transport(transport)
                .requestType(requestType)
                .targetType(targetType)
                .targetName(targetName)
                .topicName(topicName == null ? "" : topicName)
                .status(status)
                .responseReceived(responseReceived)
                .responseType(responseType == null ? "" : responseType)
                .queryText(queryText == null ? "" : queryText)
                .requestPayload(toJson(requestPayload))
                .responsePayload(toJson(responsePayload))
                .errorMessage(errorMessage == null ? "" : errorMessage)
                .requestedAt(requestedAt == null ? "" : requestedAt.toString())
                .receivedAt(receivedAt == null ? "" : receivedAt.toString())
                .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }
}
