package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.kafka.KafkaListenerService;
import com.banking_microservices.money_service.kafka.KafkaSender;
import com.banking_microservices.money_service.models.KafkaLastActivity;
import com.banking_microservices.money_service.repository.KafkaLastActivityRepository;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class TransactionService {
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final UserMoneyService service;
    private final UserMoneyRepository repository;
    private final KafkaSender kafkaSender;
    private final KafkaListenerService kafkaListenerService;
    private final KafkaLastActivityRepository kafkaLastActivityRepository;

    public TransactionService(UserMoneyService service, UserMoneyRepository repository, KafkaSender kafkaSender,
            KafkaListenerService kafkaListenerService, KafkaLastActivityRepository kafkaLastActivityRepository) {
        this.service = service;
        this.repository = repository;
        this.kafkaSender = kafkaSender;
        this.kafkaListenerService = kafkaListenerService;
        this.kafkaLastActivityRepository = kafkaLastActivityRepository;
    }
    public void blockMoney(KafkaTransactionTopicMessageDto dto){

        // kafka exception sending eklenelecek

        if(dto.getSenderIban().isEmpty()){
            dto.setError(true);
            dto.setErrorDescription("Sender Iban Not Found");
            kafkaSender.sendTransactionError(dto.getEventUUID(),dto);
            throw new IbanNotFoundException("Iban value is empty");
        }

        try{
            repository.decrementAndBlockByIban(dto.getSenderIban(), dto.getMoney());
        }catch (Exception e){
            dto.setError(true);
            dto.setErrorDescription("An Exception with decrement money and block money with iban.");
            kafkaSender.sendTransactionError(dto.getEventUUID(),dto);
            throw new DecramentAndBlockMoneyException("An exception with Decrement Money And Block money with Iban number. Iban = " + dto.getSenderIban());
        }

        dto.setIsMoneyBlocked(true);


    }
    public void KafkaTransactionTopicService(KafkaTransactionTopicMessageDto dto) {

        if (kafkaLastActivityRepository.existsByEventUUID(dto.getEventUUID())) {
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
                throw new EventSaveException("Event Save exception " + dto.getEventUUID());
            }
        }

        BigDecimal balance = repository.findBalanceByIban(dto.getSenderIban()) // ORELSEGET
                .orElseGet(() -> {
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto));
                });

        repository.findBalanceByIban(dto.getReceiverIban()) // ORELSEGET
                .orElseGet(() -> {
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto));
                });

        if (balance.compareTo(dto.getMoney()) > 0) { // bigdecimal oldugu icin boyle yazmam lazim.

            kafkaSender.sendTransactionToUserService(dto.getEventUUID(), dto);

        } else {
            throw new MoneyNotAvaibleException("Money not avaible KafkaTransactionTopicService");
        }

    }

    // Rollback Icin Transactional Onemli
    // 200 cekildi 300 cekilirken hata aldi 500 yatiriyo hesaba
    @Transactional
    public void createTransaction(KafkaTransactionTopicMessageDto dto) {

        log.info("createTransaction veri geldi. Sender Iban {}, Receiver IBAN: {}, Amount: {}",
                gson.toJson(dto.getSenderIban()), gson.toJson(dto.getReceiverIban()), gson.toJson(dto.getMoney()));

        if (dto.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Transfer Amount must be pozitife {}", gson.toJson(dto.getMoney()));
            throw new NegativeNumberException("Transfer amount must be positive");
        }

        if (dto.getReceiverIban().equals(dto.getSenderIban())) {
            log.error("The Transfer Iban and Receiver Iban Can Not Same. {}", gson.toJson(dto.getSenderIban()));
            throw new SameAccountException("Cannot transfer to the same account");
        }

        //
        // Normalde gerek yok ama tekrardan kontrol ediyoruz her ihtimale karşı
        //
        repository.findBalanceByIban(dto.getSenderIban())
                .orElseGet(() -> {
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto));
                });

        repository.findBalanceByIban(dto.getReceiverIban())
                .orElseGet(() -> {
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

            log.info("Transfer Success Sender {}, Receiver {}, Transfer Amount {}", gson.toJson(dto.getSenderIban()),
                    gson.toJson(dto.getReceiverIban()), gson.toJson(dto.getMoney()));

            dto.setStatus("SUCCESS");
            try {
                kafkaSender.sendTransaction(dto.getEventUUID(), dto);
            } catch (Exception e) {
                throw new KafkaSendException("An Error While Send End Of Transaction On Kafka");
            }

        } catch (Exception e) {
            dto.setError(true);
            dto.setErrorDescription("An Error While withdrawing money On Money-Service.");

            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

            log.warn("An Error While withdraw Money {}", gson.toJson(dto));

        }

    }

}
