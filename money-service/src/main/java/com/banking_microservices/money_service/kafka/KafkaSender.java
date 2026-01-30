package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try{
            String jsonMessageForKafka = gson.toJson(kafkaTransactionTopicMessageDto);
            kafkaTemplate.send("${kafka.topics.transaction.sender}", key, kafkaTransactionTopicMessageDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, kafkaTransactionTopicMessageDto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, kafkaTransactionTopicMessageDto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + kafkaTransactionTopicMessageDto);

        }

    }

    public void sendTransactionToUserService(String key, KafkaTransactionTopicMessageDto dto){
        try{
            String jsonMessageForKafka = gson.toJson(dto);
            kafkaTemplate.send("${kafka.topics.username-validation.sender}", key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, dto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + dto);

        }
    }

    public void sendTransactionError(String key, KafkaTransactionTopicMessageDto dto){

        try{
            String jsonMessageForKafka = gson.toJson(dto);
            kafkaTemplate.send("${kafka.topics.transaction.error}", key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, dto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + dto);

        }
    }
    public void sendCreateUserError(String key, KafkaTransactionTopicMessageDto dto){

        try{
            String jsonMessageForKafka = gson.toJson(dto);
            kafkaTemplate.send("${kafka.topics.create-user.error}", key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, dto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + dto);

        }
    }

    public void sendCreateUserSuccess(String key, KafkaTransactionTopicMessageDto dto){

        try{
            String jsonMessageForKafka = gson.toJson(dto);
            kafkaTemplate.send("${kafka.topics.create-user.sender}", key, dto);
            log.info("Kafkaya mesaj gonderildi {} {}", key, dto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, dto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + dto);

        }
    }
}
