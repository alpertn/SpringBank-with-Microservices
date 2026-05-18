package com.banking_microservices.admin_service.kafka;

import com.banking_microservices.admin_service.dto.AdminHistoryCommandRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminHistoryKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${admin-service.topics.command}")
    private String commandTopic;

    public void publish(AdminHistoryCommandRequest request) {
        try {
            kafkaTemplate.send(commandTopic, request.requestId(), objectMapper.writeValueAsString(request));
        } catch (Exception exception) {
            throw new IllegalStateException("Admin history Kafka publish failed", exception);
        }
    }
}
