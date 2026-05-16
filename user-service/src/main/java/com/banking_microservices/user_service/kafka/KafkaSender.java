package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


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

    @Value("${kafka.topics.create-user.sender}")
    private String createUserTopic;

    @Value("${kafka.topics.transaction.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.transaction.error}")
    private String transactionErrorTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate, java.util.function.Supplier<String> currentTime) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentTime = currentTime;
    }

    public void sendCreateUser(String userId) {
        try {
            log.info(" ({}) > KafkaSender | sendCreateUser -> Kafkaya mesaj gonderilmek uzere alindi. UserId: {}", currentTime.get(), userId);
            kafkaTemplate.send(createUserTopic, userId);
            log.info(" ({}) > KafkaSender | sendCreateUser -> Kafkaya mesaj gonderildi. UserId: {}", currentTime.get(), userId);
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendCreateUser -> Kafkaya mesaj gonderilirken hata olustu. UserId: {}, Hata: {}", currentTime.get(), userId, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + userId);
        }
    }

    public void sendTransactionUserValidationSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionUserValidationSuccess -> Kafkaya mesaj gonderilmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(transactionSenderTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendTransactionUserValidationSuccess -> Transaction success kafkaya mesaj gonderildi. Key: {}, Dto:\n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionUserValidationSuccess -> Transaction kafkaya mesaj gonderilirken hata olustu. Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionUsernameValidationError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionUsernameValidationError -> Kafkaya mesaj gonderilmek uzere alindi. Dto:\n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(transactionErrorTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendTransactionUsernameValidationError -> Transaction error kafkaya mesaj gonderildi. Key: {}, Dto:\n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionUsernameValidationError -> Transaction error kafkaya mesaj gonderilirken hata olustu. Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

}
