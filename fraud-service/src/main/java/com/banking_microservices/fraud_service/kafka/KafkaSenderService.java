package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import com.banking_microservices.fraud_service.dto.enums.TransactionStatus;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
public class KafkaSenderService {

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
    private String transactionSenderTopic;

    public KafkaSenderService(KafkaTemplate<String, Object> kafkaTemplate, Supplier<String> currentTime) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentTime = currentTime;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSenderService | sendTransaction -> Fraud check oncesi kafkaya gonderilmek uzere alindi. Key: {}, Dto:\n{}", currentTime.get(), key, gson.toJson(dto));
            kafkaTemplate.send(transactionSenderTopic, key, dto);
            log.info(" ({}) > KafkaSenderService | sendTransaction -> Fraud checked kafkaya gonderildi. Key: {}, Dto:\n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSenderService | sendTransaction -> Fraud send hatasi! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            dto.setStatus(TransactionStatus.FAILED);
            dto.setError(true);
            dto.setErrorDescription("Fraud transaction send failed: " + e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }
}