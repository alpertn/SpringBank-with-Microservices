package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.SagaEventDto;
import com.banking_microservices.money_service.dto.SagaEventsDto;
import com.banking_microservices.money_service.exception.KafkaSendException;
import com.banking_microservices.money_service.models.SagaEvents;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
public class KafkaSender {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();

    private final Supplier<String> currentTime;

    @Value("${kafka.topics.transaction.transactionmoney.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.username-validation.sender}")
    private String usernameValidationSenderTopic;

    @Value("${kafka.topics.transaction.error}")
    private String transactionErrorTopic;

    @Value("${kafka.topics.create-user.error}")
    private String createUserErrorTopic;

    @Value("${kafka.topics.create-user.sender}")
    private String createUserSenderTopic;

    @Value("${kafka.topics.transaction.blockmoney.sender}")
    private String blockMoneyTopicSender;

    @Value("${kafka.topics.transaction.blockmoney.user-validation-sender}")
    private String blockMoneyUserValidationTopicSender;

    @Value("${kafka.topics.transaction.saga.sender}")
    private String sagaSenderTopic;

    @Value("${kafka.topics.transaction.saga.error}")
    private String sagaErrorTopic;


    @Value("${kafka.topics.transaction.result.sender}")
    private String transactionResultSenderTopic;

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate, Supplier<String> currentTime) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentTime = currentTime;
    }



    /**
     * Unified result sender: replaces sendDepositSuccess() and sendWithdrawSuccess().
     * Sends the transaction result (COMPLETED) back to transaction-service via single result topic.
     */
    public void sendResult(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendResult -> Transaction result gonderilmek uzere alindi. Type: {}, Dto: \n{}", currentTime.get(), dto.getTransactionType(), gson.toJson(dto));
            kafkaTemplate.send(transactionResultSenderTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendResult -> Transaction result gonderildi. Key: {}, Dto: \n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendResult -> Transaction result gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            dto.setStatus(TransactionStatus.FAILED);
            dto.setError(true);
            dto.setErrorDescription("Result send failed: " + e.getMessage());
            try {
                sendTransactionError(key, dto);
            } catch (Exception ex) {
                log.error("Failed to send transaction error fallback: {}", ex.getMessage());
            }
            throw new KafkaSendException("Kafka Result Send Exception. " + key);
        }
    }

    public void sendBlockedMoneyTopic(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendBlockedMoneyTopic -> Blockmoney mesaji gonderilmek uzere alindi. Dto: \n{}", currentTime.get(), gson.toJson(dto));
            // 1) transaction-service status update
            kafkaTemplate.send(blockMoneyTopicSender, key, dto);
            // 2) user-service'in dogrudan alacagi ayri validation topic
            kafkaTemplate.send(blockMoneyUserValidationTopicSender, key, dto);
            log.info(" ({}) > KafkaSender | sendBlockedMoneyTopic -> Kafkaya blockmoney mesaji gonderildi (her iki topice). Key: {}", currentTime.get(), key);
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendBlockedMoneyTopic -> Kafkaya blockmoney mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            dto.setStatus(TransactionStatus.BLOCK_MONEY_FAILED);
            dto.setError(true);
            dto.setErrorDescription("Block money send failed: " + e.getMessage());
            try {
                sendTransactionError(key, dto);
            } catch (Exception ex) {
                log.error("Failed to send transaction error fallback: {}", ex.getMessage());
            }
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionToUserService(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionToUserService -> Kafkaya mesaj gonderilmek uzere alindi. Dto: \n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(usernameValidationSenderTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendTransactionToUserService -> Kafkaya mesaj gonderildi. Key: {}, Dto: \n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionToUserService -> Kafkaya mesaj gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionError -> Kafkaya error mesaji gonderilmek uzere alindi. Dto: \n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(transactionErrorTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendTransactionError -> Kafkaya error mesaji gonderildi. Key: {}, Dto: \n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionError -> Kafkaya error mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendCreateUserError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendCreateUserError -> Kafkaya user error mesaji gonderilmek uzere alindi. Dto: \n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(createUserErrorTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendCreateUserError -> Kafkaya user error mesaji gonderildi. Key: {}, Dto: \n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendCreateUserError -> Kafkaya user error mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendCreateUserSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendCreateUserSuccess -> Kafkaya user success mesaji gonderilmek uzere alindi. Dto: \n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(createUserSenderTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendCreateUserSuccess -> Kafkaya user success mesaji gonderildi. Key: {}, Dto: \n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendCreateUserSuccess -> Kafkaya user success mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionSuccess -> EFT transfer success gonderilmek uzere alindi. Dto: \n{}", currentTime.get(), gson.toJson(dto));
            kafkaTemplate.send(transactionSenderTopic, key, dto);
            log.info(" ({}) > KafkaSender | sendTransactionSuccess -> EFT transfer success gonderildi. Key: {}, Dto: \n{}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionSuccess -> EFT transfer success gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Transaction Success Send Exception. " + key + " " + dto);
        }
    }


    public void sendSagaSuccess(SagaEventsDto dto) {
        if (dto == null) throw new KafkaSendException("sendSagaSuccess: dto null");
        if (dto.getKafkaEventUUID() == null) throw new KafkaSendException("sendSagaSuccess: kafkaEventUUID null");

        log.info(" ({}) > KafkaSender | sendSagaSuccess -> Saga success Kafkaya gonderilmek uzere alindi. UUID: {}, Status: {}", currentTime.get(), dto.getUUID(), dto.getStatus());
        try {
            kafkaTemplate.send(sagaSenderTopic, dto.getKafkaEventUUID(), dto);
            log.info(" ({}) > KafkaSender | sendSagaSuccess -> Saga success Kafkaya gonderildi. Topic: {}, UUID: {}", currentTime.get(), sagaSenderTopic, dto.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaSender | sendSagaSuccess -> Saga success Kafkaya gonderilemedi! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
            throw new KafkaSendException("Kafka Saga Success Send Exception. UUID = " + dto.getKafkaEventUUID() + " - " + e.getMessage());
        }

    }

    public void sendSagaError(SagaEventsDto dto) {
        if (dto == null) throw new KafkaSendException("sendSagaError: dto null");
        if (dto.getKafkaEventUUID() == null) throw new KafkaSendException("sendSagaError: kafkaEventUUID null");

        log.info(" ({}) > KafkaSender | sendSagaError -> Saga error Kafkaya gonderilmek uzere alindi. UUID: {}, Status: {}", currentTime.get(), dto.getUUID(), dto.getStatus());
        try {
            kafkaTemplate.send(sagaErrorTopic, dto.getKafkaEventUUID(), dto);
            log.info(" ({}) > KafkaSender | sendSagaError -> Saga error Kafkaya gonderildi. Topic: {}, UUID: {}", currentTime.get(), sagaErrorTopic, dto.getUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaSender | sendSagaError -> Saga error Kafkaya gonderilemedi! UUID: {}, Hata: {}", currentTime.get(), dto.getUUID(), e.getMessage());
            throw new KafkaSendException("Kafka Saga Error Send Exception. UUID = " + dto.getKafkaEventUUID() + " - " + e.getMessage());
        }

    }

}