package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
            .create();

    private final Supplier<String> currentTime;

    @Value("${kafka.topics.transaction.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.transaction.deposit.sender}")
    private String depositSenderTopic;

    @Value("${kafka.topics.transaction.withdraw.sender}")
    private String withdrawSenderTopic;

    public KafkaSenderService(KafkaTemplate<String, Object> kafkaTemplate, Supplier<String> currentTime) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentTime = currentTime;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSenderService | sendTransaction -> EFT fraud check oncesi kafkaya gonderilmek uzere alindi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
            kafkaTemplate.send(transactionSenderTopic, key, dto);
            log.info(" ({}) > KafkaSenderService | sendTransaction -> EFT fraud checked kafkaya gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSenderService | sendTransaction -> EFT fraud send hatasi! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendDeposit(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSenderService | sendDeposit -> Deposit fraud check oncesi kafkaya gonderilmek uzere alindi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
            kafkaTemplate.send(depositSenderTopic, key, dto);
            log.info(" ({}) > KafkaSenderService | sendDeposit -> Deposit fraud checked kafkaya gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSenderService | sendDeposit -> Deposit fraud send hatasi! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Deposit Send Exception. " + key);
        }
    }

    public void sendWithdraw(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSenderService | sendWithdraw -> Withdraw fraud check oncesi kafkaya gonderilmek uzere alindi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
            kafkaTemplate.send(withdrawSenderTopic, key, dto);
            log.info(" ({}) > KafkaSenderService | sendWithdraw -> Withdraw fraud checked kafkaya gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSenderService | sendWithdraw -> Withdraw fraud send hatasi! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Withdraw Send Exception. " + key);
        }
    }
}