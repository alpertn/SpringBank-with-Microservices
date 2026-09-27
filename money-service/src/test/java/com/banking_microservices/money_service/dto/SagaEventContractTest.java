package com.banking_microservices.money_service.dto;

import com.banking_microservices.money_service.dto.enums.SagaStatus;
import com.banking_microservices.money_service.kafka.KafkaProducerConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SagaEventContractTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, context) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, context) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    @Test
    void kafkaPayloadUsesCanonicalTransactionField() {
        SagaEventsDto event = SagaEventsDto.builder()
                .UUID("saga-1")
                .kafkaEventUUID("event-1")
                .status(SagaStatus.COMPLETED)
                .transaction(transaction("TRANSFER"))
                .build();

        byte[] serialized = new KafkaProducerConfig.GsonSerializer().serialize("saga-result-topic", event);
        JsonObject payload = JsonParser.parseString(new String(serialized, StandardCharsets.UTF_8)).getAsJsonObject();

        assertThat(payload.has("transaction")).isTrue();
        assertThat(payload.has("transactionHistory")).isFalse();
        assertThat(payload.has("transactionEntity")).isFalse();
        assertThat(payload.getAsJsonObject("transaction").get("transactionType").getAsString())
                .isEqualTo("TRANSFER");
    }

    @Test
    void consumerAcceptsCanonicalAndLegacyTransactionFieldNames() {
        assertTransactionType("transaction");
        assertTransactionType("transactionHistory");
        assertTransactionType("transactionEntity");
    }

    @Test
    void sagaSnapshotMatchesCanonicalContract() {
        assertThat(Arrays.stream(SagaTransactionSnapshot.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(Set.of(
                        "id", "eventId", "receiverName", "senderName", "senderSurname", "senderEmail",
                        "receiverEmail", "receiverSurname", "senderUserId", "receiverUserId", "senderIban",
                        "receiverIban", "money", "transactionType", "description", "localDateTime", "error",
                        "errorDescription", "userValidation", "isMoneyBlocked", "status", "tokenDetails"));
    }

    private void assertTransactionType(String fieldName) {
        String payload = "{\"UUID\":\"saga-1\",\"kafkaEventUUID\":\"event-1\",\""
                + fieldName + "\":{\"transactionType\":\"TRANSFER\",\"money\":100.00}}";

        SagaEventsDto event = gson.fromJson(payload, SagaEventsDto.class);

        assertThat(event.getTransaction()).isNotNull();
        assertThat(event.getTransaction().getTransactionType()).isEqualTo("TRANSFER");
        assertThat(event.getTransaction().getMoney()).isEqualByComparingTo("100.00");
    }

    private SagaTransactionSnapshot transaction(String type) {
        return SagaTransactionSnapshot.builder()
                .eventId("event-1")
                .transactionType(type)
                .money(new BigDecimal("100.00"))
                .build();
    }
}
