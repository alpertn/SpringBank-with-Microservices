package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.service.service;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private service service;

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void CreateUserListener(String kafkaData) {

        KafkaTransactionTopicMessageDto transactionRequest = gson.fromJson(kafkaData,
                KafkaTransactionTopicMessageDto.class); // fromjson kullanmamiz lazim java classina cevirmemiz icin
        log.info("CreateUserListener data geldi {}", gson.toJson(kafkaData));

    }
}