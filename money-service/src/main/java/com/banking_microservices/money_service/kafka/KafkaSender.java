package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.KafkaSendException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Value("${kafka.topics.transaction.transactionmoney.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.username-validation.sender}")
    private String usernameValidationSenderTopic;

    @Value("${kafka.topics.transaction.error}")
    private String transactionErrorTopic;

    @Value("${kafka.topics.transaction.blockmoney.sender}")
    private String blockMoneyTopicSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String asJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {

            kafkaTemplate.send(transactionSenderTopic, key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, asJson(kafkaTransactionTopicMessageDto));

        } catch (Exception e) {

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, asJson(kafkaTransactionTopicMessageDto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);

        }

    }

    public void sendBlockedMoneyTopic(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {

            kafkaTemplate.send(transactionSenderTopic, key, blockMoneyTopicSender);
            log.info("Kafkaya mesaj gonderildi {} {}", key, blockMoneyTopicSender);

        } catch (Exception e) {

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, asJson(kafkaTransactionTopicMessageDto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);

        }

    }
    public void sendTransactionToUserService(String key, KafkaTransactionTopicMessageDto dto) {
        try {

            kafkaTemplate.send(usernameValidationSenderTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, asJson(dto));

        } catch (Exception e) {

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, asJson(dto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);

        }
    }

    public void sendTransactionError(String key, KafkaTransactionTopicMessageDto dto) {

        try {

            kafkaTemplate.send(transactionErrorTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, asJson(dto));
        } catch (Exception e) {

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, asJson(dto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);

        }
    }
}
