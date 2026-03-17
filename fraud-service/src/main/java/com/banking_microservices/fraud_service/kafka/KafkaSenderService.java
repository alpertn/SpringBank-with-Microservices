package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.exception.KafkaSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaSenderService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.transaction.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.transaction.deposit.sender}")
    private String depositSenderTopic;

    @Value("${kafka.topics.transaction.withdraw.sender}")
    private String withdrawSenderTopic;

    public KafkaSenderService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(transactionSenderTopic, key, dto);
            log.info("EFT fraud checked kafkaya gonderildi key={}", key);
        } catch (Exception e) {
            log.warn("EFT fraud send hatasi key={}", key);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendDeposit(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(depositSenderTopic, key, dto);
            log.info("Deposit fraud checked kafkaya gonderildi key={}", key);
        } catch (Exception e) {
            log.warn("Deposit fraud send hatasi key={}", key);
            throw new KafkaSendException("Kafka Deposit Send Exception. " + key);
        }
    }

    public void sendWithdraw(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(withdrawSenderTopic, key, dto);
            log.info("Withdraw fraud checked kafkaya gonderildi key={}", key);
        } catch (Exception e) {
            log.warn("Withdraw fraud send hatasi key={}", key);
            throw new KafkaSendException("Kafka Withdraw Send Exception. " + key);
        }
    }
}