package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.exception.KafkaSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendCreateUser(String key) {
        try{
            kafkaTemplate.send("CreateUser-Topic", key);
            log.info("Kafkaya mesaj gonderildi {} ", key);
        }catch (Exception e){
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} " , key);
            throw new KafkaSendException("Kafka Send Exception. "+ key);
        }


    }
}
