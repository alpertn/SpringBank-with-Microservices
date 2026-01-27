package com.banking_microservices.money_service.kafka;

    import com.banking_microservices.money_service.exception.KafkaSendException;
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

    public void sendTransaction(String key, TransactionRequestDto transactionRequestDto) {
        try{
            String jsonMessageForKafka = gson.toJson(transactionRequestDto);
            kafkaTemplate.send("banking-microservices.transaction-service.created.v1", key, transactionRequestDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key,transactionRequestDto);

        }catch (Exception e){

            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, transactionRequestDto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + transactionRequestDto);

        }

    }
}
