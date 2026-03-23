package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.KafkaSendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Bu class money service Kafka Sender ıcın yazılmıstır. classlar tarafından cagırılır. ve kafkaya verı gonderır..

 */
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
            .create();

    private final Supplier<String> currentTime;

    @Value("${kafka.topics.transaction.transactionmoney.sender}")
    private String transactionSenderTopic;

    @Value("${kafka.topics.transaction.deposit.sender}")
    private String depositSenderTopic;

    @Value("${kafka.topics.transaction.withdraw.sender}")
    private String withdrawSenderTopic;

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

    public KafkaSender(KafkaTemplate<String, Object> kafkaTemplate, Supplier<String> currentTime) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentTime = currentTime;
    }

    /**
     * Transaction topicine veri gonderır..
     * 
     * @param key Kafka ıcın Key Value (eventUUID).
     * @param kafkaTransactionTopicMessageDto gonderılecek asıl veri.
     * @throws KafkaSendException exception.
     */
    public void sendTransaction(String key, KafkaTransactionTopicMessageDto kafkaTransactionTopicMessageDto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransaction -> Kafkaya mesaj gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(kafkaTransactionTopicMessageDto));

            kafkaTemplate.send(transactionSenderTopic, key, kafkaTransactionTopicMessageDto);

            log.info(" ({}) > KafkaSender | sendTransaction -> Kafkaya mesaj gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(kafkaTransactionTopicMessageDto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransaction -> Kafkaya mesaj gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + kafkaTransactionTopicMessageDto);
        }
    }

    /**
     * deposit basarili olunca kafka topicine mesaj gonderir.
     * 
     * @param key eventUUID
     * @param dto gonderılen veri
     */
    public void sendDepositSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendDepositSuccess -> Deposit success gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(depositSenderTopic, key, dto);

            log.info(" ({}) > KafkaSender | sendDepositSuccess -> Deposit success gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendDepositSuccess -> Deposit success gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Deposit Success Exception. " + key);
        }
    }

    /**
     * withdraw basarili olunca kafka topicine msaj  gonderir.
     * 
     * @param key eventUUID
     * @param dto gonderılen veri
     */
    public void sendWithdrawSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendWithdrawSuccess -> Withdraw success gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(withdrawSenderTopic, key, dto);

            log.info(" ({}) > KafkaSender | sendWithdrawSuccess -> Withdraw success gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendWithdrawSuccess -> Withdraw success gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Withdraw Success Exception. " + key);
        }
    }

    /**
     * blockmoney islemi tammalandıgında topice mesaj gonderir.
     * 
     * @param key eventUUID
     * @param dto Data
     */
    public void sendBlockedMoneyTopic(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendBlockedMoneyTopic -> Blockmoney mesaji gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(blockMoneyTopicSender, key, dto);

            log.info(" ({}) > KafkaSender | sendBlockedMoneyTopic -> Kafkaya blockmoney mesaji gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendBlockedMoneyTopic -> Kafkaya blockmoney mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionToUserService(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionToUserService -> Kafkaya mesaj gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(usernameValidationSenderTopic, key, dto);

            log.info(" ({}) > KafkaSender | sendTransactionToUserService -> Kafkaya mesaj gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionToUserService -> Kafkaya mesaj gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendTransactionError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionError -> Kafkaya error mesaji gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(transactionErrorTopic, key, dto);

            log.info(" ({}) > KafkaSender | sendTransactionError -> Kafkaya error mesaji gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionError -> Kafkaya error mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendCreateUserError(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendCreateUserError -> Kafkaya user error mesaji gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(createUserErrorTopic, key, dto);

            log.info(" ({}) > KafkaSender | sendCreateUserError -> Kafkaya user error mesaji gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendCreateUserError -> Kafkaya user error mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    public void sendCreateUserSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendCreateUserSuccess -> Kafkaya user success mesaji gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(createUserSenderTopic, key, dto);

            log.info(" ({}) > KafkaSender | sendCreateUserSuccess -> Kafkaya user success mesaji gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendCreateUserSuccess -> Kafkaya user success mesaji gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Send Exception. " + key + " " + dto);
        }
    }

    /**
     * EFT transfer tamamlandiginda transaction-service success topicine mesaj gonderir.
     * {@link com.banking_microservices.money_service.service.helper.MoneyTransferExecutor}
     * tarafindan cagrilir.
     *
     * @param key eventUUID
     * @param dto gonderilen veri
     */
    public void sendTransactionSuccess(String key, KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > KafkaSender | sendTransactionSuccess -> EFT transfer success gonderilmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

            kafkaTemplate.send(transactionSenderTopic, key, dto);

            log.info(" ({}) > KafkaSender | sendTransactionSuccess -> EFT transfer success gonderildi. Key: {}, Dto: {}", currentTime.get(), key, gson.toJson(dto));
        } catch (Exception e) {
            log.warn(" ({}) > KafkaSender | sendTransactionSuccess -> EFT transfer success gonderilirken hata olustu! Key: {}, Hata: {}", currentTime.get(), key, e.getMessage());
            throw new KafkaSendException("Kafka Transaction Success Send Exception. " + key + " " + dto);
        }
    }
}