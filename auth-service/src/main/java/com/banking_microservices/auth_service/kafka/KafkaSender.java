package com.banking_microservices.auth_service.kafka;

import com.banking_microservices.auth_service.dto.CreateUserTopicDto;
import com.banking_microservices.auth_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * <p> Bu class Kafka topiclerine veri gonderir <p>
 * Kafka CreateUser  topıcıne mesaj gonderıcı.
 * {@link CreateUserTopicDto} turundeki veriyi Kafka topic'ıne gonderır.
 */
@Service
@Slf4j
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

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate, Supplier<String> currentTime) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentTime = currentTime;
    }

    @Value("${kafka.topics.createuser.sender}")
    private String createUserSenderTopic;

    /**
     * CreateUser topıcıne mesaj gonderır
     * Controller tarafından cagırılır.
     *
     *
     * @param dto createUserTopicDto
     */
    public void sendCreateUserToUserTopic(CreateUserTopicDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendCreateUserToUserTopic -> Kafkaya veri gonderilmek uzere alindi. \n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(createUserSenderTopic, dto.getKeycloackUserUUID(), dto);
            log.info(" ({}) > KafkaSender | sendCreateUserToUserTopic -> Kafka Topicine veri gonderildi. \n{}", currentTime.get(), gson.toJson(dto));

        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendCreateUserToUserTopic -> Kafka Topicine veri gonderilemedi! Hata: {}", currentTime.get(), e);
            throw new KafkaSendException("An Exception With dto to send kafka" + e);
        }
    }

}
