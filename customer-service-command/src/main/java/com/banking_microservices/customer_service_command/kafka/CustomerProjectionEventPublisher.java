package com.banking_microservices.customer_service_command.kafka;

import com.banking_microservices.customer_service_command.dto.CustomerProjectionEvent;
import com.banking_microservices.customer_service_command.exception.ProjectionPublishException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerProjectionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${customer-service.topics.projection-sync}")
    private String projectionTopic;

    public void publish(CustomerProjectionEvent event) {
        try {
            kafkaTemplate.send(projectionTopic, event.aggregateId(), objectMapper.writeValueAsString(event))
                    .get(30, TimeUnit.SECONDS);
            log.info("Customer projection event published. topic={}, eventId={}, aggregateId={}, operation={}",
                    projectionTopic, event.eventId(), event.aggregateId(), event.operationType());
        } catch (Exception exception) {
            throw new ProjectionPublishException("Customer projection event could not be published for eventId=" + event.eventId(), exception);
        }
    }
}
