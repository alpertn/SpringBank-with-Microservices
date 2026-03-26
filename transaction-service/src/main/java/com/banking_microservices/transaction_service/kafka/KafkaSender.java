package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import com.banking_microservices.transaction_service.dto.enums.TransactionStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
    private final java.util.function.Supplier<String> currentTime;

    @Value("${kafka.topics.transaction.sender}")
    private String transactionCreateTopic;

    @Value("${kafka.topics.transaction.deposit.sender}")
    private String transactionDepositTopic;

    @Value("${kafka.topics.transaction.withdraw.sender}")
    private String transactionWithdrawTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate, java.util.function.Supplier<String> currentTime) {
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

    public void sendDeposit(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {
            log.info(" ({}) > KafkaSender | sendDeposit -> Kafkaya deposit mesaji gonderilmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(kafkaTransactionTopicMessageDto));
            kafkaTemplate.send(transactionDepositTopic, key, kafkaTransactionTopicMessageDto);
            log.info(" ({}) > KafkaSender | sendDeposit -> Kafkaya deposit mesaji gonderildi. Key: {}, Dto:\n{}", currentTime.get(), key, gson.toJson(kafkaTransactionTopicMessageDto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendDeposit -> Kafkaya deposit mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            kafkaTransactionTopicMessageDto.setStatus(TransactionStatus.DEPOSIT_FAILED);
            kafkaTransactionTopicMessageDto.setError(true);
            kafkaTransactionTopicMessageDto.setErrorDescription("Deposit send failed: " + e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }

    public void sendWithdraw(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {
            log.info(" ({}) > KafkaSender | sendWithdraw -> Kafkaya withdraw mesaji gonderilmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(kafkaTransactionTopicMessageDto));
            kafkaTemplate.send(transactionWithdrawTopic, key, kafkaTransactionTopicMessageDto);
            log.info(" ({}) > KafkaSender | sendWithdraw -> Kafkaya withdraw mesaji gonderildi. Key: {}, Dto:\n{}", currentTime.get(), key, gson.toJson(kafkaTransactionTopicMessageDto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendWithdraw -> Kafkaya withdraw mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            kafkaTransactionTopicMessageDto.setStatus(TransactionStatus.WITHDRAW_FAILED);
            kafkaTransactionTopicMessageDto.setError(true);
            kafkaTransactionTopicMessageDto.setErrorDescription("Withdraw send failed: " + e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }
}