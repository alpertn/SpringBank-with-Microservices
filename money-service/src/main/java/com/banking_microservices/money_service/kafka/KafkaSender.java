package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.KafkaSendException;
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
    // DUZELTME: gson field'i birkac metodda "jsonMessageForKafka" dead code uretmek icin kullaniliyordu.
    // Dead code satirlari temizlendi, gson artik kullanilmadigi icin kaldirildi.

    @Value("${kafka.topics.transaction.transactionmoney.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.transaction.deposit.sender}")
    private String depositSenderTopic;

    @Value("${kafka.topics.transaction.withdraw.sender}")
    private String withdrawSenderTopic;

    @Value("${kafka.topics.username-validation.sender}")
    private String usernameValidationSenderTopic;

    @Value("${kafka.topics.transaction.error}")
    private String transactionErrorTopic;

    @Value("${kafka.topics.create-user.error}")
    private String createUserErrorTopic;

    @Value("${kafka.topics.create-user.sender}")
    private String createUserSenderTopic;

    @Value("${kafka.topics.transaction.blockmoney.sender}")
    private String blockMoneyTopicSender;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {
            // DUZELTME: jsonMessageForKafka dead code satiri kaldirildi.
            kafkaTemplate.send(transactionSenderTopic, key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, kafkaTransactionTopicMessageDto);
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, kafkaTransactionTopicMessageDto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }

    public void sendDepositSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(depositSenderTopic, key, dto);
            log.info("Deposit success gonderildi: {}", key);
        } catch (Exception e) {
            log.warn("Deposit success gonderilirken hata: {}", e.getMessage());
            throw new KafkaSendException("Kafka Deposit Success Exception. " + key);
        }
    }

    public void sendWithdrawSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(withdrawSenderTopic, key, dto);
            log.info("Withdraw success gonderildi: {}", key);
        } catch (Exception e) {
            log.warn("Withdraw success gonderilirken hata: {}", e.getMessage());
            throw new KafkaSendException("Kafka Withdraw Success Exception. " + key);
        }
    }

    public void sendBlockedMoneyTopic(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            kafkaTemplate.send(blockMoneyTopicSender, key, dto);
            log.info("Kafkaya blockmoney mesaji gonderildi {} {}", key, dto);
        } catch (Exception e) {
            log.warn("Kafkaya blockmoney mesaji gonderilirken hata olustu {} {}", key, dto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionToUserService(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            // DUZELTME: jsonMessageForKafka dead code satiri kaldirildi.
            kafkaTemplate.send(usernameValidationSenderTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            // DUZELTME: jsonMessageForKafka dead code satiri kaldirildi.
            kafkaTemplate.send(transactionErrorTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendCreateUserError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            // DUZELTME: jsonMessageForKafka dead code satiri kaldirildi.
            kafkaTemplate.send(createUserErrorTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendCreateUserSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            // DUZELTME: jsonMessageForKafka dead code satiri kaldirildi.
            kafkaTemplate.send(createUserSenderTopic, key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);
        } catch (Exception e) {
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, dto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }
}