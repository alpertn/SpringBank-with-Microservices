package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.KafkaEventType;
import com.banking_microservices.money_service.exception.EventUUIDAlreadyExists;
import com.banking_microservices.money_service.kafka.KafkaSender;
import com.banking_microservices.money_service.service.helper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Bu sinif {@link IbanResolver}, {@link TransactionValidator}, {@link IdempotencyGuard},
 * {@link BlockMoneyService}, {@link MoneyTransferExecutor} ve {@link KafkaSender} siniflarini cagirir.
 *
 * Para transfer surecleri icin ana orkestrasyon servisidir.
 * Bu sinif yalnizca koordinasyon yapar. Validasyon, IBAN cozumleme,
 * bloke etme, transfer ve idempotency islemleri helper classlara delege edilir.
 *
 * Orijinal method imzalari korundugu icin Kafka listenerlar ve diger cagiran taraflar bozulmaz.
 */
@Slf4j
@Service
public class TransactionService {

    private final IbanResolver ibanResolver;
    private final TransactionValidator validator;
    private final IdempotencyGuard idempotencyGuard;
    private final BlockMoneyService blockMoneyService;
    private final MoneyTransferExecutor moneyTransferExecutor;
    private final KafkaSender kafkaSender;
    private final Supplier<String> currentTime;

    public TransactionService(IbanResolver ibanResolver,
                              TransactionValidator validator,
                              IdempotencyGuard idempotencyGuard,
                              BlockMoneyService blockMoneyService,
                              MoneyTransferExecutor moneyTransferExecutor,
                              KafkaSender kafkaSender,
                              Supplier<String> currentTime) {
        this.ibanResolver = ibanResolver;
        this.validator = validator;
        this.idempotencyGuard = idempotencyGuard;
        this.blockMoneyService = blockMoneyService;
        this.moneyTransferExecutor = moneyTransferExecutor;
        this.kafkaSender = kafkaSender;
        this.currentTime = currentTime;
    }

    /**
     * Kafkadan gelen para bloke etme istegini isler.
     *
     * 1 - Sender IBAN cozumlenir. gerekirse userId uzerinden
     * 2 - Sender IBAN varligi dogrulanir
     * 3 - Receiver IBAN userId cozumlenir ve dtoya set edilir
     * 4 - DBde para bloke edilir ve Kafkaya BLOCK_MONEY gonderilir
     *
     * @param dto Kafkadan gelen islem DTOsu
     */
    public void KafkaTransactionTopicBlockMoney(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > TransactionService | KafkaTransactionTopicBlockMoney -> Metoda veri geldi. {}", currentTime.get(), dto);

        ibanResolver.resolveSenderIban(dto);
        ibanResolver.assertSenderIbanExists(dto);

        String receiverUserId = ibanResolver.resolveReceiverUserIdOrThrow(dto);
        dto.setReceiverUserId(receiverUserId);

        blockMoneyService.blockFunds(dto);

        log.info(" ({}) > TransactionService | KafkaTransactionTopicBlockMoney -> Islem tamamlandi. EventUUID: {}", currentTime.get(), dto.getEventUUID());
    }

    /**
     * Kafkadan gelen on validasyon ve bakiye kontrol istegini isler.
     * Yeterli bakiye varsa islemi user-service'e iletir.
     *
     * 1 - Idempotency kontrolu. duplicate UUID reject
     * 2 - Sender IBAN cozumlenir. gerekirse userId uzerinden
     * 3 - Sender IBAN varligi dogrulanir
     * 4 - Sender bakiyesi sorgulanir
     * 5 - Receiver hesabi varligi dogrulanir
     * 6 - Bakiye yeterliligi kontrol edilir. bakiye miktardan buyuk olmali
     * 7 - Kafkaya sendTransactionToUserService gonderilir
     *
     * @param dto Kafkadan gelen islem DTOsu
     * @throws EventUUIDAlreadyExists ayni UUID daha once islendiyse firlatir
     */
    public void KafkaTransactionTopicService(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > TransactionService | KafkaTransactionTopicService -> Metoda veri geldi. {}", currentTime.get(), dto);

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.TRANSACTION_TOPIC_SERVICE.name())) {
            throw new EventUUIDAlreadyExists("Event UUID Already exists KafkaTransactionTopicService " + dto.getEventUUID());
        }

        ibanResolver.resolveSenderIban(dto);
        ibanResolver.assertSenderIbanExists(dto);

        BigDecimal balance = ibanResolver.getBalanceOrThrow(dto.getSenderIban(), "Sender", dto);
        ibanResolver.assertAccountExists(dto.getReceiverIban(), "Receiver", dto);

        validator.assertSufficientBalance(balance, dto);

        log.info(" ({}) > TransactionService | KafkaTransactionTopicService -> Bakiye yeterli. Kafkaya gonderiliyor. {}", currentTime.get(), dto);

        kafkaSender.sendTransactionToUserService(dto.getEventUUID(), dto);
    }

    /**
     * Fiili para transferini gerceklestirir. withdraw ve deposit.
     *
     * 1 - Is kurali validasyonu. miktar sifirdan buyuk olmali ve sender receiver farkli olmali
     * 2 - Sender ve Receiver hesaplari dogrulanir
     * 3 - Para cekilir, yatirilir ve Kafkaya COMPLETED gonderilir
     *
     * @Transactional withdraw ve deposit ayni DB transactioninda. biri basarisiz olursa rollback.
     *
     * @param dto Kafkadan gelen islem DTOsu
     */
    @Transactional
    public void createTransaction(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > TransactionService | createTransaction -> Metoda veri geldi. Sender Iban: {}, Receiver IBAN: {}, Amount: {}", currentTime.get(), dto.getSenderIban(), dto.getReceiverIban(), dto.getMoney());

        validator.assertAmountIsPositive(dto);
        validator.assertNotSameAccount(dto);

        ibanResolver.assertAccountExists(dto.getSenderIban(), "Sender", dto);
        ibanResolver.assertAccountExists(dto.getReceiverIban(), "Receiver", dto);

        moneyTransferExecutor.execute(dto);
    }
}
