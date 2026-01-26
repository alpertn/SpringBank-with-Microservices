package com.banking_microservices.transaction_service.kafka;


import com.banking_microservices.transaction_service.dto.TransactionRequestDto;
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

    //public class TopicConstants {
    //    public static final String TRANSFER_CREATED = "banking.transaction.transfer-created.v1";
    //    public static final String BALANCE_CHANGED = "banking.account.balance-changed.v1";
    //    public static final String USER_VERIFIED = "banking.user.identity-verified.v1";
    //}
    public void sendTransaction(String key, TransactionRequestDto transactionRequestDto) {
        try{
            kafkaTemplate.send("Transaction-Topic", key, transactionRequestDto);
            log.info("Kafkaya mesaj gonderildi {} {}", key,transactionRequestDto);
        }catch (Exception e){
            log.warn("Kafkaya mesaj godnerilirken hata olustu {} {}" , key, transactionRequestDto);
            throw new KafkaSendException("Kafka Send Exception. "+ key +" " + transactionRequestDto);
        }

    }
}
