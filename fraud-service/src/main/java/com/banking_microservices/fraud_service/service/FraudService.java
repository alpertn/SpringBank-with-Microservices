package com.banking_microservices.fraud_service.service;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.kafka.KafkaSenderService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Slf4j
@Service
public class FraudService {

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

    private final KafkaSenderService kafkaSender;
    private final Supplier<String> currentTime;

    public FraudService(KafkaSenderService kafkaSender, Supplier<String> currentTime) {
        this.kafkaSender = kafkaSender;
        this.currentTime = currentTime;
    }

    public void send(KafkaTransactionTopicMessageDto requestDto) {
        log.info(" ({}) > FraudService | send -> Metoda veri geldi.\n{}", currentTime.get(), gson.toJson(requestDto));
        kafkaSender.sendTransaction(requestDto.getEventUUID(), requestDto);
        log.info(" ({}) > FraudService | send -> Kafkaya gonderildi.\n{}", currentTime.get(), gson.toJson(requestDto));
    }
}