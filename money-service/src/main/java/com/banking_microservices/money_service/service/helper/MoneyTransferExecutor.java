package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.dto.enums.TransactionStatus;
import com.banking_microservices.money_service.exception.DeposItOrWithdrawFailedException;
import com.banking_microservices.money_service.exception.KafkaSendException;
import com.banking_microservices.money_service.kafka.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Bu class {@link BalanceOperationService}, {@link KafkaSender} ve {@link TransactionErrorHandler} classlarini cagirir.
 *
 * Para transferinin fiili olarak calistirilmasindan sorumludur.
 * 1 - Senderdan para cek
 * 2 - Receivera para yatir
 * 3 - Kafkaya COMPLETED gonder
 *
 * withdraw deposit hatasi olursa Kafkaya error gonderilir ve exception firlatilir.
 * Kafka COMPLETED gonderimi basarisizsa sadece KafkaSendException firlatilir, error gonderilmez.
 * Para zaten transfer edildi error gondermek yanlis rollback izlenimi yaratir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyTransferExecutor {

    private final BalanceOperationService balanceOperationService;
    private final KafkaSender kafkaSender;
    private final TransactionErrorHandler errorHandler;
    private final Supplier<String> currentTime;

    /**
     * Para transferini gerceklestirir ve tamamlandiginda Kafkaya bildirir.
     *
     * @param dto islem DTOsu
     * @throws DeposItOrWithdrawFailedException withdraw veya deposit basarisizsa firlatir
     * @throws KafkaSendException transfer basarili ama Kafka bildirimi basarisizsa firlatir
     */
    public void execute(KafkaTransactionTopicMessageDto dto) {
        // para transferi. basarisizsa Kafkaya error gonderilir.
        try {
            balanceOperationService.withdrawMoneyByIban(dto.getSenderIban(), dto.getMoney());
            balanceOperationService.depositMoneyByIban(dto.getReceiverIban(), dto.getMoney());
        } catch (Exception e) {
            log.error(" ({}) > MoneyTransferExecutor | execute -> Para cekme veya yatirma sirasinda hata olustu! {}", currentTime.get(), e.getMessage());
            errorHandler.sendErrorAndThrow(dto, "An Error While withdrawing money On Money-service.", new DeposItOrWithdrawFailedException("Deposit or withdraw failed on Money-service createTransaction " + e.getMessage()));
        }

        // transfer basarili. DTO guncelle.
        log.info(" ({}) > MoneyTransferExecutor | execute -> Transfer basariyla gerceklesti. Sender: {}, Receiver: {}, Amount: {}", currentTime.get(), dto.getSenderIban(), dto.getReceiverIban(), dto.getMoney());

        dto.setStatus(TransactionStatus.COMPLETED);
        dto.setStatusDescription(TransactionStatus.COMPLETED.getDescription());

        // Kafkaya COMPLETED bildir. basarisizsa sadece KafkaSendException firlatilir. error gonderilmez.
        try {
            log.info(" ({}) > MoneyTransferExecutor | execute -> Transfer complete kafkaya gonderiliyor. {}", currentTime.get(), dto);
            kafkaSender.sendTransactionSuccess(dto.getEventUUID(), dto);
        } catch (Exception e) {
            log.error(" ({}) > MoneyTransferExecutor | execute -> Kafkaya transfer tamamlandi mesaji gonderilemedi! Hata: {}", currentTime.get(), e.getMessage());
            throw new KafkaSendException("An Error While Send End Of Transaction On Kafka");
        }
    }
}
