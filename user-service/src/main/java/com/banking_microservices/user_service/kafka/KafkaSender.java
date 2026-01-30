package com.banking_microservices.user_service.kafka;

import com.banking_microservices.user_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    public void sendCreateUser(String userId) { // overload sayesinde keysiz de kullanabiliyoruz
        try{
            kafkaTemplate.send("${kafka.topics.create-user.sender}", userId);
            log.info("sendCreateUser mesaj gonderildi {} ", userId);
        }catch (Exception e){
            log.warn("sendCreateUser Kafkaya mesaj godnerilirken hata olustu {} " , userId);
            throw new KafkaSendException("Kafka Send Exception. "+ userId);
        }


    }

    public void sendUsernameValidationSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try{
            String jsonMessageForKafka = gson.toJson(dto);
            kafkaTemplate.send("${kafka.topics.username-validation.sender}", key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, dto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + dto);

        }

    }
    public void sendUsernameValidationError(String key, KafkaTransactionTopicMessageDto dto) {
        try{
            String jsonMessageForKafka = gson.toJson(dto);
            kafkaTemplate.send("${kafka.topics.username-validation.error}", key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, dto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + dto);

        }

    }
}
