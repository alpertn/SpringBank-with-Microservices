package com.banking_microservices.auth_service.kafka;

import com.banking_microservices.auth_service.dto.CreateUserTopicDto;
import com.banking_microservices.auth_service.exception.KafkaSendException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${kafka.topics.createuser.sender}")
    private String createUserSenderTopic;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String asJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    public void sendCreateUserToUserTopic(CreateUserTopicDto dto) {
        try {
            kafkaTemplate.send(createUserSenderTopic, dto.getKeycloackUserUUID(), dto);
            log.info("Kafkaya mesaj gonderildi {} {}", dto.getKeycloackUserUUID(), asJson(dto));
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", dto.getKeycloackUserUUID(), asJson(dto));
            throw new KafkaSendException("An Exception With dto to send kafka" + e.getMessage());
        }
    }

}
// @Slf4j
// @Service
// public class KafkaSender {
//
// private final KafkaTemplate<String, Object> kafkaTemplate;
//
// @Value("${kafka.topics.create-user.sender}")
// private String createUserTopic;
//
// @Value("${kafka.topics.username-validation.sender}")
// private String usernameValidationSuccessTopic;
//
// @Value("${kafka.topics.username-validation.error}")
// private String usernameValidationErrorTopic;
//
// public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
// this.kafkaTemplate = kafkaTemplate;
// }
//
// public void sendCreateUser(String userId) {
// try {
// kafkaTemplate.send(createUserTopic, userId);
// log.info("sendCreateUser mesaj gonderildi {} ", userId);
// } catch (Exception e) {
// log.warn("sendCreateUser Kafkaya mesaj godnerilirken hata olustu {} ",
// userId);
// throw new KafkaSendException("Kafka Send Exception. " + userId);
// }
// }
//
// public void sendUsernameValidationSuccess(String key,
// KafkaTransactionTopicMessageDto dto) {
// try {
// kafkaTemplate.send(usernameValidationSuccessTopic, key, dto);
// log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
// } catch (Exception e) {
// log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
// throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
// }
// }
//
// public void sendUsernameValidationError(String key,
// KafkaTransactionTopicMessageDto dto) {
// try {
// kafkaTemplate.send(usernameValidationErrorTopic, key, dto);
// log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
// } catch (Exception e) {
// log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
// throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
// }
// }
// }