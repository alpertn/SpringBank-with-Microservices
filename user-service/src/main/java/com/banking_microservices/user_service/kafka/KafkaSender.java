package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.exception.KafkaSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String asJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    @Value("${kafka.topics.username-validation.sender}")
    private String usernameValidationSuccessTopic;

    @Value("${kafka.topics.username-validation.error}")
    private String usernameValidationErrorTopic;

    @Value("${kafka.topics.transaction.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.transaction.error}")
    private String transactionErrorTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendTransactionUserValidationSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(transactionSenderTopic, key, dto);
            log.info("Transaction success kafkaya mesaj gonderildi {} {}", key, asJson(dto));
        } catch (Exception e) {
            log.warn("Transaction kafkaya mesaj gonderilirken hata olustu {} {}", key, asJson(dto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionUsernameValidationError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(transactionErrorTopic, key, dto);
            log.info("Transaction error kafkaya mesaj gonderildi {} {}", key, asJson(dto));
        } catch (Exception e) {
            log.warn("Transaction error kafkaya mesaj gonderilirken hata olustu {} {}", key, asJson(dto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendUsernameValidationSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(usernameValidationSuccessTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, asJson(dto));
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, asJson(dto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendUsernameValidationError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(usernameValidationErrorTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, asJson(dto));
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, asJson(dto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }
}
