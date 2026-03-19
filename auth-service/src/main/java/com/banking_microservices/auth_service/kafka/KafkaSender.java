package com.banking_microservices.auth_service.kafka;

import com.banking_microservices.auth_service.dto.CreateUserTopicDto;
import com.banking_microservices.auth_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;

/**
 * Kafka Topic Sender class
 * <p> Bu class Kafka topiclerine veri gonderir <p>
 */
@Service
@Slf4j
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${kafka.topics.createuser.sender}")
    private String createUserSenderTopic;

    /**
     * Kafka CreateUser  topıcıne mesaj gonderıcı.
     * Bu class Controller tarafından Cagırılıyor.
     *
     * {@link CreateUserTopicDto} turundeki veriyi Kafka topic'ıne gonderır.
     * @param dto createUserTopıcDto
     */
    public void sendCreateUserToUserTopic(CreateUserTopicDto dto) {
        try {
            log.info("sendCreateUserToUserTopic Class veri geldi. Auth-Service" + dto.getKeycloackUserUUID());
            kafkaTemplate.send(createUserSenderTopic, dto.getKeycloackUserUUID(), dto);
            log.info("sendCreateUserToUserTopic Class Kafkaya veri gonderildi. Auth-Service" + dto.getKeycloackUserUUID());

        } catch (Exception e) {
            log.warn("sendCreateUserToUserTopic Class Kafkaya veri Gonderilemedi. Auth-Service" + dto.getKeycloackUserUUID());
            throw new KafkaSendException("An Exception With dto to send kafka" + e.getMessage());
        }
    }

}
