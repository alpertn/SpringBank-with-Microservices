package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.kafka.KafkaListenerService;
import com.banking_microservices.money_service.kafka.KafkaSender;
import org.springframework.context.annotation.Lazy;
import com.banking_microservices.money_service.models.KafkaLastActivity;
import com.banking_microservices.money_service.repository.KafkaLastActivityRepository;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Slf4j
@Service
public class TransactionService {
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
    private final UserMoneyService service;
    private final UserMoneyRepository repository;
    private final KafkaSender kafkaSender;
    private final KafkaListenerService kafkaListenerService;
    private final KafkaLastActivityRepository kafkaLastActivityRepository;

    public TransactionService(UserMoneyService service, UserMoneyRepository repository, KafkaSender kafkaSender,
            @Lazy KafkaListenerService kafkaListenerService, KafkaLastActivityRepository kafkaLastActivityRepository, Supplier<String> currentTime) {
        this.service = service;
        this.repository = repository;
        this.kafkaSender = kafkaSender;
        this.kafkaListenerService = kafkaListenerService;
        this.kafkaLastActivityRepository = kafkaLastActivityRepository;
        this.currentTime = currentTime;
    }

    public void KafkaTransactionTopicBlockMoney(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > TransactionService | KafkaTransactionTopicBlockMoney -> Metoda veri geldi. {}", currentTime.get(), gson.toJson(dto));

        // kafka exception sending eklenelecek

        if ((dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) && dto.getSenderUserId() != null) {
            dto.setSenderIban(repository.findIbanByUserId(dto.getSenderUserId()).orElse(null));
        }

        if (dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) {
            log.warn(" ({}) > TransactionService | KafkaTransactionTopicBlockMoney -> Sender Iban bulunamadi! {}", currentTime.get(), gson.toJson(dto));
            dto.setError(true);
            dto.setErrorDescription("Sender Iban Not Found");
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            throw new IbanNotFoundException("Iban value is empty");
        }

        String receiverId = repository.findUserIdByIban(dto.getReceiverIban()).orElse(null);

        if (receiverId == null) {
            log.warn(" ({}) > TransactionService | KafkaTransactionTopicBlockMoney -> Receiver Iban bulunamadi! {}", currentTime.get(), dto.getReceiverIban());
            dto.setError(true);
            dto.setErrorDescription("Receiver Iban Not Found: " + dto.getReceiverIban());

            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

            throw new IbanNotFoundException(
                    "KafkaTransactionTopicBlockMoney Hesap bulunamadi." + gson.toJson(dto));
        }

        dto.setReceiverUserId(receiverId);

        try {
            repository.decrementAndBlockByIban(dto.getSenderIban(), dto.getMoney());
            dto.setIsMoneyBlocked(true);
            dto.setStatus(TransactionStatus.FUNDS_BLOCKED);
            dto.setStatusDescription(TransactionStatus.FUNDS_BLOCKED.getDescription());
            log.info(" ({}) > TransactionService | KafkaTransactionTopicBlockMoney -> Para bloke edildi ve Kafkaya gonderiliyor. {}", currentTime.get(), gson.toJson(dto));
            kafkaSender.sendBlockedMoneyTopic(dto.getEventUUID(), dto);
        } catch (Exception e) {
            log.error(" ({}) > TransactionService | KafkaTransactionTopicBlockMoney -> Para bloke edilirken hata olustu! Hata: {}", currentTime.get(), e.getMessage());
            dto.setError(true);
            dto.setErrorDescription("An Exception with decrement money and block money with iban.");
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            throw new DecramentAndBlockMoneyException(
                    "An exception with Decrement Money And Block money with Iban number. Iban = "
                            + dto.getSenderIban());
        }

    }

    public void KafkaTransactionTopicService(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > TransactionService | KafkaTransactionTopicService -> Metoda veri geldi. {}", currentTime.get(), gson.toJson(dto));

        if (kafkaLastActivityRepository.existsByEventUUID(dto.getEventUUID())) {
            log.warn(" ({}) > TransactionService | KafkaTransactionTopicService -> Event UUID zaten mevcut! {}", currentTime.get(), dto.getEventUUID());
            throw new EventUUIDAlreadyExists(
                    "Event UUID Already exists KafkaTransactionTopicService " + dto.getEventUUID());
        } else {
            // varsa hata yoksa save ve continue
            // backtrack two sum gibi.
            try {
                kafkaLastActivityRepository.save(
                        KafkaLastActivity
                                .builder()
                                .eventUUID(dto.getEventUUID())
                                .build());
            } catch (Exception e) {
                log.error(" ({}) > TransactionService | KafkaTransactionTopicService -> Event kayit edilemedi! {}", currentTime.get(), dto.getEventUUID());
                throw new EventSaveException("Event Save exception " + dto.getEventUUID());
            }
        }

        if ((dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) && dto.getSenderUserId() != null) {
            dto.setSenderIban(repository.findIbanByUserId(dto.getSenderUserId()).orElse(null));
        }

        if (dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) {
            log.warn(" ({}) > TransactionService | KafkaTransactionTopicService -> Sender Iban bulunamadi! {}", currentTime.get(), gson.toJson(dto));
            dto.setError(true);
            dto.setErrorDescription("Sender Iban Not Found");
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            throw new IbanNotFoundException("Iban value is empty");
        }

        BigDecimal balance = repository.findBalanceByIban(dto.getSenderIban()) // ORELSEGET
                .orElseGet(() -> {
                    log.warn(" ({}) > TransactionService | KafkaTransactionTopicService -> Sender Hesap bulunamadi! {}", currentTime.get(), dto.getSenderIban());
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto));
                });

        repository.findBalanceByIban(dto.getReceiverIban()) // ORELSEGET
                .orElseGet(() -> {
                    log.warn(" ({}) > TransactionService | KafkaTransactionTopicService -> Receiver Hesap bulunamadi! {}", currentTime.get(), dto.getReceiverIban());
                    dto.setError(true);
                    dto.setErrorDescription("Receiver Iban Number Not Found: " + dto.getReceiverIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Receiver Hesap bulunamadi." + gson.toJson(dto));
                });

        if (balance.compareTo(dto.getMoney()) > 0) { // bigdecimal oldugu icin boyle yazmam lazim.
            log.info(" ({}) > TransactionService | KafkaTransactionTopicService -> Bakiye yeterli. Kafkaya gonderiliyor. {}", currentTime.get(), gson.toJson(dto));
            kafkaSender.sendTransactionToUserService(dto.getEventUUID(), dto);

        } else {
            log.warn(" ({}) > TransactionService | KafkaTransactionTopicService -> Bakiye yetersiz! Bankadaki miktar: {} | Istenilen miktar: {}", currentTime.get(), balance, dto.getMoney());
            throw new MoneyNotAvaibleException("Money not avaible KafkaTransactionTopicService");
        }

    }

    // Rollback Icin Transactional Onemli
    // 200 cekildi 300 cekilirken hata aldi 500 yatiriyo hesaba
    @Transactional
    public void createTransaction(KafkaTransactionTopicMessageDto dto) {

        log.info(" ({}) > TransactionService | createTransaction -> Metoda veri geldi. Sender Iban: {}, Receiver IBAN: {}, Amount: {}", currentTime.get(), gson.toJson(dto.getSenderIban()), gson.toJson(dto.getReceiverIban()), gson.toJson(dto.getMoney()));

        if (dto.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            log.error(" ({}) > TransactionService | createTransaction -> Transfer miktari pozitif olmalidir! Amount: {}", currentTime.get(), gson.toJson(dto.getMoney()));
            throw new NegativeNumberException("Transfer amount must be positive");
        }

        if (dto.getReceiverIban().equals(dto.getSenderIban())) {
            log.error(" ({}) > TransactionService | createTransaction -> Gonderen ve Alici Iban ayni olamaz! {}", currentTime.get(), gson.toJson(dto.getSenderIban()));
            throw new SameAccountException("Cannot transfer to the same account");
        }

        //
        // Normalde gerek yok ama tekrardan kontrol ediyoruz her ihtimale karşı
        //
        repository.findBalanceByIban(dto.getSenderIban())
                .orElseGet(() -> {
                    log.warn(" ({}) > TransactionService | createTransaction -> Sender Hesap bulunamadi! {}", currentTime.get(), dto.getSenderIban());
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto));
                });

        repository.findBalanceByIban(dto.getReceiverIban())
                .orElseGet(() -> {
                    log.warn(" ({}) > TransactionService | createTransaction -> Receiver Hesap bulunamadi! {}", currentTime.get(), dto.getReceiverIban());
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto));
                });

        //
        //
        //

        try {
            service.withdrawMoneyByIban(dto.getSenderIban(), dto.getMoney());

            service.depositMoneyByIban(dto.getReceiverIban(), dto.getMoney());

            log.info(" ({}) > TransactionService | createTransaction -> Transfer basariyla gerceklesti. Sender: {}, Receiver: {}, Amount: {}", currentTime.get(), gson.toJson(dto.getSenderIban()), gson.toJson(dto.getReceiverIban()), gson.toJson(dto.getMoney()));

            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());
            try {
                log.info(" ({}) > TransactionService | createTransaction -> Transfer complete kafkaya gonderiliyor. {}", currentTime.get(), gson.toJson(dto));
                kafkaSender.sendTransaction(dto.getEventUUID(), dto);
            } catch (Exception e) {
                log.error(" ({}) > TransactionService | createTransaction -> Kafkaya transfer tamamlandi mesaji gonderilemedi! Hata: {}", currentTime.get(), e.getMessage());
                throw new KafkaSendException("An Error While Send End Of Transaction On Kafka");
            }

        } catch (Exception e) {
            log.error(" ({}) > TransactionService | createTransaction -> Para cekme veya yatirma sirasinda hata olustu! {}", currentTime.get(), e.getMessage());
            dto.setError(true);
            dto.setErrorDescription("An Error While withdrawing money On Money-service.");

            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

            log.warn(" ({}) > TransactionService | createTransaction -> An Error While withdraw Money {}", currentTime.get(), gson.toJson(dto));
            throw new DeposItOrWithdrawFailedException("Deposit or withdraw failed on Money-service createTransaction " + e.getMessage());
        }

    }

}
