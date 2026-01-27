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


    public void sendCreateUser(String userId) { // overload sayesinde keysiz de kullanabiliyoruz
        try{
            kafkaTemplate.send("CreateUser-Topic", userId);
            log.info("sendCreateUser mesaj gonderildi {} ", userId);
        }catch (Exception e){
            log.warn("sendCreateUser Kafkaya mesaj godnerilirken hata olustu {} " , userId);
            throw new KafkaSendException("Kafka Send Exception. "+ userId);
        }


    }
}
