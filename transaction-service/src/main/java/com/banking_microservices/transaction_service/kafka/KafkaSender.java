package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.exception.KafkaSendException;
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
            .create();

    @Value("${kafka.topics.transaction.sender}")
    private String transactionCreateTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
            String jsonMessageForKafka = gson.toJson(kafkaTransactionTopicMessageDto);
            kafkaTemplate.send(transactionCreateTopic, key,
                    kafkaTransactionTopicMessageDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, kafkaTransactionTopicMessageDto);

        } catch (Exception e) {

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}", key, kafkaTransactionTopicMessageDto);
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);

        }

    }
}
