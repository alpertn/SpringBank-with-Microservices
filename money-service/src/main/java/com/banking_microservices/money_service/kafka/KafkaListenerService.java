package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.TransactionRequestDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaListenerService {
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @KafkaListener(topics = "${kafka.topics.transaction.listener}")
    public void listenTransactionTopic(String topicData){
        TransactionRequestDto dto = gson.fromJson(topicData, TransactionRequestDto.class);
    }

}
//private final Gson gson = new GsonBuilder().serializeNulls().create();
//private UserMoneyService UserMoneyService;
//
//@KafkaListener(topics = "${kafka.topics.transaction.sender}")
//public void CreateUserListener(String kafkaData){
//
//    TransactionRequestDto transactionRequest = gson.fromJson(kafkaData, TransactionRequestDto.class); // fromjson kullanmamiz lazim java classina cevirmemiz icin
//    log.info("CreateUserListener data geldi {}", gson.toJson(kafkaData));
//
//
//}