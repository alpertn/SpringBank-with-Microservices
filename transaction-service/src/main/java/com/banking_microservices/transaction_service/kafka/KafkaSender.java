package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.KafkaSendException;
import com.banking_microservices.transaction_service.exception.ValueNotFoundException;
import com.banking_microservices.transaction_service.model.SagaEvents;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import com.banking_microservices.transaction_service.dto.enums.TransactionStatus;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
public class KafkaSender {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();
    private final Supplier<String> currentTime;

    @Value("${kafka.topics.transaction.sender}")
    private String transactionCreateTopic;

    @Value("${kafka.topics.transaction.saga.sender}")
    private String sagaSenderTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate, Supplier<String> currentTime) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentTime = currentTime;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransaction -> Kafkaya mesaj gonderilmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(kafkaTransactionTopicMessageDto));
            kafkaTemplate.send(transactionCreateTopic, key, kafkaTransactionTopicMessageDto);
            log.info(" ({}) > KafkaSender | sendTransaction -> Kafkaya mesaj gonderildi. Key: {}, Dto:\n{}", currentTime.get(), key, gson.toJson(kafkaTransactionTopicMessageDto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransaction -> Kafkaya mesaj gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            kafkaTransactionTopicMessageDto.setStatus(TransactionStatus.FAILED);
            kafkaTransactionTopicMessageDto.setError(true);
            kafkaTransactionTopicMessageDto.setErrorDescription("Transfer request send failed: " + e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }

    public void sendSagaEvent(SagaEvents dto) {
        if (dto == null) throw new ValueNotFoundException("Values can not null");
        if (dto.getKafkaEventUUID() == null || dto.getUUID() == null || dto.getTransactionHistory() == null)
            throw new ValueNotFoundException("Değerler Boş olamaz. " + dto);

        log.info(" ({}) > KafkaSender | sendSagaEvent -> Saga event Kafkaya gonderilmek uzere alindi. UUID: {}, Status: {}", currentTime.get(), dto.getUUID(), dto.getStatus());

        try {
            kafkaTemplate.send(sagaSenderTopic, dto.getKafkaEventUUID(), dto);
            log.info(" ({}) > KafkaSender | sendSagaEvent -> Saga event Kafkaya gonderildi. Topic: {}, UUID: {}", currentTime.get(), sagaSenderTopic, dto.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaSender | sendSagaEvent -> Saga event Kafkaya gonderilemedi! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
            throw new KafkaSendException("Kafka Send Exception (Saga). UUID: " + dto.getUUID() + " - " + e.getMessage());
        }

    }
}