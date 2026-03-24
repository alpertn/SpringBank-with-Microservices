package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class IbanGeneratorService {

    private final UserMoneyRepository userMoneyRepository;
    private final Supplier<String> currentTime;

    public String generateUniqueTurkishIban() {
        String iban;
        do {
            iban = Iban.random(CountryCode.TR).toString();
        } while (userMoneyRepository.existsByUserIban(iban));
        log.debug(" ({}) > IbanGeneratorService | generateUniqueTurkishIban -> Yeni IBAN uretildi. {}", currentTime.get(), iban);
        return iban;
    }
}
