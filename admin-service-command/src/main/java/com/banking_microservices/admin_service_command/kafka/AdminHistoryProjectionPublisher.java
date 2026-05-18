package com.banking_microservices.admin_service_command.kafka;

import com.banking_microservices.admin_service_command.dto.AdminHistoryProjectionEvent;
import com.banking_microservices.admin_service_command.exception.AdminCommandException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminHistoryProjectionPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${admin-service.topics.projection-sync}")
    private String projectionTopic;

    public void publish(AdminHistoryProjectionEvent event) {
        try {
            kafkaTemplate.send(projectionTopic, event.getRequestId(), objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            throw new AdminCommandException("Admin projection event publish failed", exception);
        }
    }
}
