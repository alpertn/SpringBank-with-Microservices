package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.exception.KafkaSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.create-user.sender}")
    private String createUserTopic;

    @Value("${kafka.topics.username-validation.sender}")
    private String usernameValidationSuccessTopic;

    @Value("${kafka.topics.username-validation.error}")
    private String usernameValidationErrorTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCreateUser(String userId) {
        try {
            kafkaTemplate.send(createUserTopic, userId);
            log.info("sendCreateUser mesaj gonderildi {} ", userId);
        } catch (Exception e) {
            log.warn("sendCreateUser Kafkaya mesaj godnerilirken hata olustu {} ", userId);
            throw new KafkaSendException("Kafka Send Exception. " + userId);
        }
    }

    public void sendUsernameValidationSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(usernameValidationSuccessTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendUsernameValidationError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(usernameValidationErrorTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }
}
