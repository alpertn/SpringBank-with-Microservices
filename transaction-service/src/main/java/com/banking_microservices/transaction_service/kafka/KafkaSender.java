package com.banking_microservices.transaction_service.kafka;


import com.banking_microservices.transaction_service.exception.KafkaSendException;
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


    public void sendTransaction(String key, String message) {
        try{
            kafkaTemplate.send("Transaction-Topic", key, message);
            log.info("Kafkaya mesaj gonderildi {} {}", key,message);
        }catch (Exception e){
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, message);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + message);
        }


    }
}
