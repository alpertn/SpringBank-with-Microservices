package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.exception.KafkaSendException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class KafkaSenderService {


    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaSenderService(KafkaTemplate<String, Object> kafkaTemplate) {
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

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try{


            kafkaTemplate.send("${kafka.topics.transaction.sender}", key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, asJson(kafkaTransactionTopicMessageDto));

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, asJson(kafkaTransactionTopicMessageDto));
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + kafkaTransactionTopicMessageDto);

        }

    }


}


