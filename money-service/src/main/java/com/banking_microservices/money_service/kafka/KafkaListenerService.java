package com.banking_microservices.money_service.kafka;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.KafkaEventType;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.service.TransactionService;
import com.banking_microservices.money_service.service.UserMoneyService;
import com.banking_microservices.money_service.service.helper.IdempotencyGuard;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Bu class {@link TransactionService}, {@link UserMoneyService} ve {@link KafkaSender} classlarini cagirir.
 *
 * Kafka topiclerini dinler ve gelen mesajlari ilgili servislere yonlendirir.
 *
 * Sorumluluklar:
 * Mesaji JSONdan DTOya donusturme
 * Temel validasyon. null UUID ve zorunlu alan kontrolu
 * Idempotency kontrolu. {@link IdempotencyGuard} uzerinden. Manuel repository cagrisi yok.
 * Ilgili service metoduna yonlendirme
 *
 * Dinlenen topicler: transaction, deposit, withdraw, block-money, username-validation
 */
@Slf4j
@Service
public class KafkaListenerService {

    private final TransactionService transactionService;
    private final UserMoneyService userMoneyService;
    private final KafkaSender kafkaSender;
    private final IdempotencyGuard idempotencyGuard;
    private final Supplier<String> currentTime;

    /**
     * Gson LocalDateTime serialize deserialize destegi.
     * Kafka mesajlarinda LocalDateTime alanlari icin gerekli.
     */
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) -> LocalDateTime.parse(json.getAsString()))
            .create();

    public KafkaListenerService(TransactionService transactionService,
                                UserMoneyService userMoneyService,
                                KafkaSender kafkaSender,
                                IdempotencyGuard idempotencyGuard,
                                Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.userMoneyService = userMoneyService;
        this.kafkaSender = kafkaSender;
        this.idempotencyGuard = idempotencyGuard;
        this.currentTime = currentTime;
    }

    /**
     * Fraud service tarafindan onaylanan EFT topicini dinler.
     * Fraud check sonrasi {@link TransactionService#createTransaction(KafkaTransactionTopicMessageDto)} cagirarak
     * para transferini gerceklestirir ve success topicine bildirim gonderir.
     *
     * @param topicData Producer tarafindan yollanan kafka verisi. DTOya cevrilip diger classlara iletilir.
     */
    @KafkaListener(topics = "${kafka.topics.transaction.transactionmoney.listener}")
    public void listenFraudCheckedTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.FRAUD_CHECKED_EFT.name())) return;

        log.info(" ({}) > KafkaListenerService | listenFraudCheckedTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        transactionService.createTransaction(dto);
    }

    /**
     * Transaction Deposit topicini dinler.
     * Gelen veriyi okuyup {@link UserMoneyService#depositMoneyByIban(String, java.math.BigDecimal)} cagirarak para yatirir.
     * Sonucu Kafkaya bildirir.
     *
     * @param topicData Producer tarafindan yollanan string formatindaki veridir. DTOya cevrilip baska classlara gonderilir.
     */
    @KafkaListener(topics = "${kafka.topics.transaction.deposit.listener}")
    public void listenDepositTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenDepositTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (dto.getReceiverIban() == null) {
            log.warn(" ({}) > KafkaListenerService | listenDepositTopic -> receiverIban null, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            return;
        }

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.DEPOSIT_PROCESS.name())) return;

        log.info(" ({}) > KafkaListenerService | listenDepositTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        try {
            userMoneyService.depositMoneyByIban(dto.getReceiverIban(), dto.getMoney());

            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());

            kafkaSender.sendDepositSuccess(dto.getEventUUID(), dto);
            log.info(" ({}) > KafkaListenerService | listenDepositTopic -> Deposit tamamlandi ve transaction-service bilgilendirildi: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenDepositTopic -> Deposit basarisiz: {}", currentTime.get(), e.getMessage());
            dto.setError(true);
            dto.setErrorDescription("Deposit islemi basarisiz: " + e.getMessage());
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
        }
    }

    /**
     * Transaction Withdraw topicini dinler.
     * {@link UserMoneyService#withdrawMoneyByIban(String, java.math.BigDecimal)} cagirarak para ceker. Sonucu Kafkaya bildirir.
     * senderIban bossa receiverIban ile fallback denenir. Bazi servisler withdrawu receiverIban olarak set edebilir.
     *
     * @param topicData Producer tarafindan yollanan string formatindaki veridir. DTOya cevrilip baska classlara gonderilir.
     */
    @KafkaListener(topics = "${kafka.topics.transaction.withdraw.listener}")
    public void listenWithdrawTopic(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (isMissing(dto.getSenderIban())) {
            log.warn(" ({}) > KafkaListenerService | listenWithdrawTopic -> senderIban bos/null, atlaniyor. Dto: {}", currentTime.get(), gson.toJson(dto));

            // fallback receiverIban dene. bazi servislerde withdraw de receiverIban olarak set ediliyor.
            if (!isMissing(dto.getReceiverIban())) {
                log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> receiverIban kullanilarak devam ediliyor: {}", currentTime.get(), dto.getReceiverIban());
                dto.setSenderIban(dto.getReceiverIban());
            } else {
                return;
            }
        }

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.WITHDRAW_PROCESS.name())) return;

        log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        try {
            userMoneyService.withdrawMoneyByIban(dto.getSenderIban(), dto.getMoney());

            dto.setStatus(TransactionStatus.COMPLETED);
            dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());

            kafkaSender.sendWithdrawSuccess(dto.getEventUUID(), dto);
            log.info(" ({}) > KafkaListenerService | listenWithdrawTopic -> Withdraw tamamlandi ve transaction-service bilgilendirildi: {}", currentTime.get(), dto.getEventUUID());
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | listenWithdrawTopic -> Withdraw basarisiz: {}", currentTime.get(), e.getMessage());
            dto.setError(true);
            dto.setErrorDescription("Withdraw islemi basarisiz: " + e.getMessage());
            kafkaSender.sendTransactionError(dto.getEventUUID(), dto);
        }
    }

    /**
     * Transaction Service tarafindan olusturulan EFT icin para bloke etme topicini dinler.
     * {@link TransactionService#KafkaTransactionTopicBlockMoney(KafkaTransactionTopicMessageDto)} idaresine birakir.
     * Yeni akis: TX-Service → Block Money → Fraud → Transfer
     *
     * @param topicData Producer tarafindan yollanan string formatindaki veridir. DTOya cevrilip baska classlara gonderilir.
     */
    @KafkaListener(topics = "${kafka.topics.transaction.blockmoney.listener}")
    public void listenBlockMoneyFromTxService(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.BLOCK_MONEY.name())) return;

        log.info(" ({}) > KafkaListenerService | listenBlockMoneyFromTxService -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        transactionService.KafkaTransactionTopicBlockMoney(dto);
    }

    /**
     * Username validation topicini dinler.
     * Su an yalnizca idempotency kaydi yapilir. Is mantigi ilerleyen sureclerde eklenecek.
     *
     * @param topicData Producer tarafindan yollanan string formatindaki veridir.
     */
    @KafkaListener(topics = "${kafka.topics.username-validation.listener}")
    public void listenUserValidationTopicOnUserService(String topicData) {
        log.info(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Metoda veri geldi. RawData: {}", currentTime.get(), topicData);

        KafkaTransactionTopicMessageDto dto = parseMessage(topicData);
        if (dto == null) return;

        if (idempotencyGuard.isDuplicateOrRegister(dto.getEventUUID(), KafkaEventType.USERNAME_VALIDATION.name())) return;

        log.info(" ({}) > KafkaListenerService | listenUserValidationTopicOnUserService -> Data islenmek uzere alindi. Dto: {}", currentTime.get(), gson.toJson(dto));

        // TODO: username validation is mantigi buraya eklenecek
    }

    /**
     * Ham JSON stringi DTOya donusturur.
     * Donusum basarisiz olursa veya eventUUID null gelirse null doner. Cagiran taraf atlar.
     *
     * @param topicData Kafkadan gelen ham json string
     * @return {@link KafkaTransactionTopicMessageDto} veya null
     */
    private KafkaTransactionTopicMessageDto parseMessage(String topicData) {
        try {
            KafkaTransactionTopicMessageDto dto = gson.fromJson(topicData, KafkaTransactionTopicMessageDto.class);
            if (dto == null || dto.getEventUUID() == null) {
                log.warn(" ({}) > KafkaListenerService | parseMessage -> Gecersiz mesaj alindi, atlaniyor. RawData: {}", currentTime.get(), topicData);
                return null;
            }
            return dto;
        } catch (Exception e) {
            log.error(" ({}) > KafkaListenerService | parseMessage -> JSON parse hatasi! RawData: {}, Hata: {}", currentTime.get(), topicData, e.getMessage());
            return null;
        }
    }

    private boolean isMissing(String value) {
        return value == null || value.trim().isEmpty();
    }
}