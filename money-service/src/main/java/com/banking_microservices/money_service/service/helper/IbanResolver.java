package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.IbanNotFoundException;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Bu class {@link UserMoneyRepository} ve {@link TransactionErrorHandler} classlarini cagirir.
 *
 * Kafka transaction akisinda IBAN dogrulama ve cozumleme islemlerini yonetir.
 * Sender userId IBAN donusumu, IBAN varlik kontrolu ve bakiye sorgulama bu classtan yapilir.
 *
 * NOT: Controller akisindaki bakiye sorguları {@link AccountQueryService} uzerinden gider.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IbanResolver {

    private final UserMoneyRepository repository;
    private final TransactionErrorHandler errorHandler;
    private final Supplier<String> currentTime;

    /**
     * Eger senderIban bossa ve senderUserId varsa, userId uzerinden IBANi resolve eder.
     * Ikisi de yoksa dokunmaz. assertSenderIbanExists sonraki adimda yakalar.
     *
     * @param dto islem DTOsu
     */
    public void resolveSenderIban(KafkaTransactionTopicMessageDto dto) {
        if (isMissing(dto.getSenderIban()) && dto.getSenderUserId() != null) {
            String resolvedIban = repository.findIbanByUserId(dto.getSenderUserId()).orElse(null);
            dto.setSenderIban(resolvedIban);
            log.info(" ({}) > IbanResolver | resolveSenderIban -> SenderIban userId uzerinden resolve edildi. UserId: {}, Iban: {}", currentTime.get(), dto.getSenderUserId(), resolvedIban);
        }
    }

    /**
     * Sender IBANin bos olmadigini dogrular.
     * Bossa Kafkaya hata gonderir ve exception firlatir.
     *
     * @param dto islem DTOsu
     * @throws IbanNotFoundException sender iban bos veya null ise
     */
    public void assertSenderIbanExists(KafkaTransactionTopicMessageDto dto) {
        if (isMissing(dto.getSenderIban())) {
            log.warn(" ({}) > IbanResolver | assertSenderIbanExists -> Sender IBAN bulunamadi! {}", currentTime.get(), dto);
            errorHandler.sendErrorAndThrow(dto, "Sender Iban Not Found", new IbanNotFoundException("Iban value is empty"));
        }
    }

    /**
     * Receiver IBANa ait userId bulur ve doner.
     * Bulunamazsa Kafkaya hata gonderir ve exception firlatir.
     *
     * @param dto islem DTOsu
     * @return receiverUserId string
     * @throws IbanNotFoundException receiver iban veritabaninda bulunamazsa
     */
    public String resolveReceiverUserIdOrThrow(KafkaTransactionTopicMessageDto dto) {
        String receiverUserId = repository.findUserIdByIban(dto.getReceiverIban()).orElse(null);
        if (receiverUserId == null) {
            log.warn(" ({}) > IbanResolver | resolveReceiverUserIdOrThrow -> Receiver IBAN bulunamadi! IBAN: {}", currentTime.get(), dto.getReceiverIban());
            errorHandler.sendErrorAndThrow(dto, "Receiver Iban Not Found: " + dto.getReceiverIban(), new IbanNotFoundException("Receiver IBAN bulunamadi: " + dto.getReceiverIban()));
        }
        return receiverUserId;
    }

    /**
     * Verilen IBANin bakiyesini doner. Hesap yoksa Kafkaya hata gonderir ve exception firlatir.
     *
     * @param iban sorgulanacak IBAN
     * @param role log ve hata mesajinda kullanilir. Sender veya Receiver
     * @param dto  islem DTOsu
     * @return bakiye {@link BigDecimal}
     * @throws IbanNotFoundException hesap bulunamazsa
     */
    public BigDecimal getBalanceOrThrow(String iban, String role, KafkaTransactionTopicMessageDto dto) {
        BigDecimal balance = repository.findBalanceByIban(iban).orElse(null);
        if (balance == null) {
            log.warn(" ({}) > IbanResolver | getBalanceOrThrow -> {} hesabi bulunamadi! IBAN: {}", currentTime.get(), role, iban);
            errorHandler.sendErrorAndThrow(dto, role + " Iban Not Found: " + iban, new IbanNotFoundException(role + " hesabi bulunamadi: " + iban));
        }
        return balance;
    }

    /**
     * Hesap varligini kontrol eder. Bakiye degeri kullanilmayacaksa bu method kullanilir.
     * Yoksa Kafkaya hata gonderir ve exception firlatir.
     *
     * @param iban sorgulanacak IBAN
     * @param role log ve hata mesajinda kullanilir. Sender veya Receiver
     * @param dto  islem DTOsu
     * @throws IbanNotFoundException hesap bulunamazsa
     */
    public void assertAccountExists(String iban, String role, KafkaTransactionTopicMessageDto dto) {
        getBalanceOrThrow(iban, role, dto);
    }

    private boolean isMissing(String iban) {
        return iban == null || iban.isEmpty();
    }
}
