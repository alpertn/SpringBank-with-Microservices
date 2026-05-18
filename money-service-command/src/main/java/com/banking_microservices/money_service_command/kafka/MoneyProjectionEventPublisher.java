package com.banking_microservices.money_service_command.kafka;

import com.banking_microservices.money_service_command.dto.MoneyProjectionEvent;
import com.banking_microservices.money_service_command.exception.ProjectionPublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyProjectionEventPublisher {

    // Kafka burada event bus gorevi gorur.
    // Command servisi read tarafina direkt DB yazmaz; projection event uretir.
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${money-service.topics.projection-sync}")
    private String projectionTopic;

    public void publish(MoneyProjectionEvent event) {
        try {
            String payload = toJson(event);
            kafkaTemplate.send(projectionTopic, event.aggregateId(), payload).get(30, TimeUnit.SECONDS);
            log.info("Projection event published. topic={}, eventId={}, aggregateId={}, operation={}",
                    projectionTopic, event.eventId(), event.aggregateId(), event.operationType());
        } catch (Exception exception) {
            throw new ProjectionPublishException("Projection event could not be published for eventId=" + event.eventId(), exception);
        }
    }

    private String toJson(MoneyProjectionEvent event) {
        return new StringBuilder(256)
                .append('{')
                .append(jsonField("eventId", event.eventId())).append(',')
                .append(jsonField("aggregateId", event.aggregateId())).append(',')
                .append(jsonField("userId", event.userId())).append(',')
                .append(jsonField("keycloakUserUUID", event.keycloakUserUUID())).append(',')
                .append(jsonField("userIban", event.userIban())).append(',')
                .append(numberField("availableBalance", event.availableBalance())).append(',')
                .append(numberField("blockedBalance", event.blockedBalance())).append(',')
                .append(jsonField("operationType", event.operationType())).append(',')
                .append(jsonField("occurredAt", event.occurredAt())).append(',')
                .append(jsonField("sourceService", event.sourceService()))
                .append('}')
                .toString();
    }

    private String jsonField(String name, String value) {
        return "\"" + escape(name) + "\":" + (value == null ? "null" : "\"" + escape(value) + "\"");
    }

    private String numberField(String name, Object value) {
        return "\"" + escape(name) + "\":" + (value == null ? "null" : value.toString());
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
