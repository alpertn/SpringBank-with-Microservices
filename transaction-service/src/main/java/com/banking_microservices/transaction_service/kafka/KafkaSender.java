package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.KafkaSendException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class KafkaSender {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.transaction.sender}")
    private String transactionCreateTopic;

    @Value("${kafka.topics.transaction.deposit.sender}")
    private String transactionDepositTopic;

    @Value("${kafka.topics.transaction.withdraw.sender}")
    private String transactionWithdrawTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String asJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    // public class TopicConstants {
    // public static final String TRANSFER_CREATED =
    // "banking.transaction.transfer-created.v1";
    // public static final String BALANCE_CHANGED =
    // "banking.account.balance-changed.v1";
    // public static final String USER_VERIFIED =
    // "banking.user.identity-verified.v1";
    // }
    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {

            kafkaTemplate.send(transactionCreateTopic, key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, asJson(kafkaTransactionTopicMessageDto));
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, asJson(kafkaTransactionTopicMessageDto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }

    public void sendDeposit(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {
            kafkaTemplate.send(transactionDepositTopic, key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya deposit mesaji gonderildi {} {}", key, asJson(kafkaTransactionTopicMessageDto));
        } catch (Exception e) {
            log.warn("Kafkaya deposit mesaji godnerilirken hata olustu {} {}", key, asJson(kafkaTransactionTopicMessageDto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }

    public void sendWithdraw(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {
            kafkaTemplate.send(transactionWithdrawTopic, key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya withdraw mesaji gonderildi {} {}", key, asJson(kafkaTransactionTopicMessageDto));
        } catch (Exception e) {
            log.warn("Kafkaya withdraw mesaji godnerilirken hata olustu {} {}", key, asJson(kafkaTransactionTopicMessageDto));
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }
}
