package com.banking_microservices.fraud_service.kafka;

import com.banking_microservices.fraud_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.fraud_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaSenderService {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaSenderService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try{

            String jsonMessageForKafka = gson.toJson(kafkaTransactionTopicMessageDto); // nulllar da gozukmesi icin bu lazim.
            kafkaTemplate.send("${kafka.topics.transaction.sender}", key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, kafkaTransactionTopicMessageDto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, kafkaTransactionTopicMessageDto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + kafkaTransactionTopicMessageDto);

        }

    }


}


