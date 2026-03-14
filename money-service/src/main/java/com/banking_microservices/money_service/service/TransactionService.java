package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.kafka.KafkaListenerService;
import com.banking_microservices.money_service.kafka.KafkaSender;
import org.springframework.context.annotation.Lazy;
import com.banking_microservices.money_service.models.KafkaLastActivity;
import com.banking_microservices.money_service.repository.KafkaLastActivityRepository;
import com.banking_microservices.money_service.repository.UserMoneyRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class TransactionService {

    private final UserMoneyService service;
    private final UserMoneyRepository repository;
    private final KafkaSender kafkaSender;
    private final KafkaLastActivityRepository kafkaLastActivityRepository;

    public TransactionService(UserMoneyService service, UserMoneyRepository repository, KafkaSender kafkaSender,
                              KafkaLastActivityRepository kafkaLastActivityRepository) {
        this.service = service;
        this.repository = repository;
        this.kafkaSender = kafkaSender;
        this.kafkaLastActivityRepository = kafkaLastActivityRepository;
    }

    public void KafkaTransactionTopicBlockMoney(KafkaTransactionTopicMessageDto dto) {

        // kafka exception sending eklenelecek

        if ((dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) && dto.getSenderUserId() != null) {
            dto.setSenderIban(repository.findIbanByUserId(dto.getSenderUserId()).orElse(null));
        }

        if (dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) {
            dto.setError(true);
            dto.setErrorDescription("Sender Iban Not Found");
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            throw new IbanNotFoundException("Iban value is empty");
        }

        String receiverId = repository.findUserIdByIban(dto.getReceiverIban()).orElse(null);

        if (receiverId == null) {
            dto.setError(true);
            dto.setErrorDescription("Receiver Iban Not Found: " + dto.getReceiverIban());

            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

            throw new IbanNotFoundException(
                    "KafkaTransactionTopicBlockMoney Hesap bulunamadi." + dto);
        }

        dto.setReceiverUserId(receiverId);

        try {
            repository.decrementAndBlockByIban(dto.getSenderIban(), dto.getMoney());
            dto.setIsMoneyBlocked(true);
            kafkaSender.sendBlockedMoneyTopic(dto.getEventUUID(), dto);
        } catch (Exception e) {
            dto.setError(true);
            dto.setErrorDescription("An Exception with decrement money and block money with iban.");
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            throw new DecramentAndBlockMoneyException(
                    "An exception with Decrement Money And Block money with Iban number. Iban = "
                            + dto.getSenderIban());
        }

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

        if ((dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) && dto.getSenderUserId() != null) {
            dto.setSenderIban(repository.findIbanByUserId(dto.getSenderUserId()).orElse(null));
        }

        if (dto.getSenderIban() == null || dto.getSenderIban().isEmpty()) {
            dto.setError(true);
            dto.setErrorDescription("Sender Iban Not Found");
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
            throw new IbanNotFoundException("Iban value is empty");
        }

        BigDecimal balance = repository.findBalanceByIban(dto.getSenderIban()) // ORELSEGET
                .orElseGet(() -> {
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + dto);
                });

        repository.findBalanceByIban(dto.getReceiverIban()) // ORELSEGET
                .orElseGet(() -> {
                    dto.setError(true);
                    dto.setErrorDescription("Receiver Iban Number Not Found: " + dto.getReceiverIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Receiver Hesap bulunamadi." + dto);
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
                dto.getSenderIban(), dto.getReceiverIban(), dto.getMoney());

        if (dto.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Transfer Amount must be pozitife {}", dto.getMoney());
            throw new NegativeNumberException("Transfer amount must be positive");
        }

        if (dto.getReceiverIban().equals(dto.getSenderIban())) {
            log.error("The Transfer Iban and Receiver Iban Can Not Same. {}", dto.getSenderIban());
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
                            "KafkaTransactionTopicService Hesap bulunamadi." + dto);
                });

        repository.findBalanceByIban(dto.getReceiverIban())
                .orElseGet(() -> {
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException(
                            "KafkaTransactionTopicService Hesap bulunamadi." + dto);
                });

        //
        //
        //

        try {
            service.withdrawMoneyByIban(dto.getSenderIban(), dto.getMoney());

            service.depositMoneyByIban(dto.getReceiverIban(), dto.getMoney());

            log.info("Transfer Success Sender {}, Receiver {}, Transfer Amount {}", dto.getSenderIban(),
                    dto.getReceiverIban(), dto.getMoney());

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

            log.warn("An Error While withdraw Money {}", dto);

        }

    }

}
