package com.banking_microservices.transaction_service.kafka;

import com.banking_microservices.transaction_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.transaction_service.service.TransactionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final TransactionService transactionService;

    public KafkaListenerService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData) {
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        transactionService.saveTransaction(dto);
    }


    @KafkaListener(topics = "${kafka.topics.transaction.error}")
    public void listenErrorTopic(String topicData){
        KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
        transactionService.saveTransaction(dto);
    }

}

