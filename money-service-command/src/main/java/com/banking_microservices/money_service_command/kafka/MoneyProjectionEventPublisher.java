package com.banking_microservices.money_service_command.kafka;

import com.banking_microservices.money_service_command.dto.MoneyProjectionEvent;
import com.banking_microservices.money_service_command.exception.ProjectionPublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyProjectionEventPublisher {

    // Kafka burada event bus gorevi gorur.
    // Command servisi read tarafina direkt DB yazmaz; projection event uretir.
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${money-service.topics.projection-sync}")
    private String projectionTopic;

    public void publish(MoneyProjectionEvent event) {
        try {
            kafkaTemplate.send(projectionTopic, event.aggregateId(), event);
            log.info("Projection event published. topic={}, eventId={}, aggregateId={}, operation={}",
                    projectionTopic, event.eventId(), event.aggregateId(), event.operationType());
        } catch (Exception exception) {
            throw new ProjectionPublishException("Projection event could not be published for eventId=" + event.eventId(), exception);
        }
    }
}
