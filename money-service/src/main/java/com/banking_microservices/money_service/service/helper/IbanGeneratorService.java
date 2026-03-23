package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Bu class {@link UserMoneyRepository} classini cagirir.
 *
 * Benzersiz Turkiye IBAN uretimini yonetir.
 * Veritabaninda mevcut olmayan bir IBAN bulunana kadar uretim tekrarlar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IbanGeneratorService {

    private final UserMoneyRepository userMoneyRepository;
    private final Supplier<String> currentTime;

    /**
     * Rastgele, veritabaninda benzersiz bir Turkiye IBANi uretir.
     *
     * @return veritabaninda bulunmayan yeni IBAN string
     */
    public String generateUniqueTurkishIban() {
        String iban;
        do {
            iban = Iban.random(CountryCode.TR).toString();
        } while (userMoneyRepository.existsByUserIban(iban));
        log.debug(" ({}) > IbanGeneratorService | generateUniqueTurkishIban -> Yeni IBAN uretildi. {}", currentTime.get(), iban);
        return iban;
    }
}
