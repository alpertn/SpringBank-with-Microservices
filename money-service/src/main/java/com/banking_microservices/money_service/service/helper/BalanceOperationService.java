package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.exception.DepositFailedException;
import com.banking_microservices.money_service.exception.IbanNotFoundException;
import com.banking_microservices.money_service.exception.MoneyNotAvaibleException;
import com.banking_microservices.money_service.exception.NegativeNumberException;
import com.banking_microservices.money_service.exception.UserNotFoundException;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceOperationService {

    private final UserMoneyRepository userMoneyRepository;
    private final Supplier<String> currentTime;

    @Transactional
    public void depositMoneyById(String id, BigDecimal amount) {
        log.info(" ({}) > BalanceOperationService | depositMoneyById -> Para yatirma istegi. ID: {}, Miktar: {}", currentTime.get(), id, amount);

        int result = userMoneyRepository.incrementBalanceById(id, amount);
        if (result == 0) {
            log.error(" ({}) > BalanceOperationService | depositMoneyById -> Para yatirma basarisiz. ID bulunamadi! {}", currentTime.get(), id);
            throw new DepositFailedException("Deposit failed. ID not found: " + id);
        }

        log.info(" ({}) > BalanceOperationService | depositMoneyById -> Para yatirma basarili. ID: {}", currentTime.get(), id);
    }

    @Transactional
    public void depositMoneyByIban(String iban, BigDecimal amount) {
        log.info(" ({}) > BalanceOperationService | depositMoneyByIban -> Para yatirma istegi. IBAN: {}, Miktar: {}", currentTime.get(), iban, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegativeNumberException("Deposit amount must be positive");
        }

        int result = userMoneyRepository.incrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error(" ({}) > BalanceOperationService | depositMoneyByIban -> Para yatirma basarisiz. IBAN bulunamadi! {}", currentTime.get(), iban);
            throw new IbanNotFoundException("Deposit failed. IBAN not found: " + iban);
        }

        log.info(" ({}) > BalanceOperationService | depositMoneyByIban -> Para yatirma basarili. IBAN: {}", currentTime.get(), iban);
    }

    @Transactional
    public void depositMoneyByUserId(String userId, BigDecimal amount) {
        log.info(" ({}) > BalanceOperationService | depositMoneyByUserId -> Para yatirma istegi. UserId: {}, Miktar: {}", currentTime.get(), userId, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegativeNumberException("Deposit amount must be positive");
        }

        int result = userMoneyRepository.incrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error(" ({}) > BalanceOperationService | depositMoneyByUserId -> Para yatirma basarisiz. UserId bulunamadi! {}", currentTime.get(), userId);
            throw new UserNotFoundException("Deposit failed. User ID not found: " + userId);
        }

        log.info(" ({}) > BalanceOperationService | depositMoneyByUserId -> Para yatirma basarili. UserId: {}", currentTime.get(), userId);
    }

    @Transactional
    public void withdrawMoneyById(String id, BigDecimal amount) {
        log.info(" ({}) > BalanceOperationService | withdrawMoneyById -> Para cekme istegi. ID: {}, Miktar: {}", currentTime.get(), id, amount);

        BigDecimal currentBalance = userMoneyRepository.findBalanceById(id).orElseThrow(() -> {
            log.warn(" ({}) > BalanceOperationService | withdrawMoneyById -> Bakiye bilgisi bulunamadi! ID: {}", currentTime.get(), id);
            return new UserNotFoundException("Balance info not found for ID: " + id);
        });

        if (currentBalance.compareTo(amount) < 0) {
            log.warn(" ({}) > BalanceOperationService | withdrawMoneyById -> Yetersiz bakiye. ID: {}, Mevcut: {}, Istened: {}", currentTime.get(), id, currentBalance, amount);
            throw new MoneyNotAvaibleException("Insufficient funds for ID: " + id);
        }

        int result = userMoneyRepository.decrementBalanceById(id, amount);
        if (result == 0) {
            log.error(" ({}) > BalanceOperationService | withdrawMoneyById -> Para cekme basarisiz. ID bulunamadi! {}", currentTime.get(), id);
            throw new UserNotFoundException("Withdraw failed. ID not found: " + id);
        }

        log.info(" ({}) > BalanceOperationService | withdrawMoneyById -> Para cekme basarili. ID: {}", currentTime.get(), id);
    }

    @Transactional
    public void withdrawMoneyByIban(String iban, BigDecimal amount) {
        log.info(" ({}) > BalanceOperationService | withdrawMoneyByIban -> Para cekme istegi. IBAN: {}, Miktar: {}", currentTime.get(), iban, amount);

        BigDecimal currentBalance = userMoneyRepository.findBalanceByIban(iban).orElseThrow(() -> {
            log.warn(" ({}) > BalanceOperationService | withdrawMoneyByIban -> Bakiye bilgisi bulunamadi! IBAN: {}", currentTime.get(), iban);
            return new IbanNotFoundException("Balance info not found for IBAN: " + iban);
        });

        if (currentBalance.compareTo(amount) < 0) {
            log.warn(" ({}) > BalanceOperationService | withdrawMoneyByIban -> Yetersiz bakiye. IBAN: {}, Mevcut: {}, Istened: {}", currentTime.get(), iban, currentBalance, amount);
            throw new MoneyNotAvaibleException("Insufficient funds for IBAN: " + iban);
        }

        int result = userMoneyRepository.decrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error(" ({}) > BalanceOperationService | withdrawMoneyByIban -> Para cekme basarisiz. IBAN bulunamadi! {}", currentTime.get(), iban);
            throw new IbanNotFoundException("Withdraw failed. IBAN not found: " + iban);
        }

        log.info(" ({}) > BalanceOperationService | withdrawMoneyByIban -> Para cekme basarili. IBAN: {}", currentTime.get(), iban);
    }

    @Transactional
    public void withdrawMoneyByUserId(String userId, BigDecimal amount) {
        log.info(" ({}) > BalanceOperationService | withdrawMoneyByUserId -> Para cekme istegi. UserId: {}, Miktar: {}", currentTime.get(), userId, amount);

        BigDecimal currentBalance = userMoneyRepository.findBalanceByUserId(userId).orElseThrow(() -> {
            log.warn(" ({}) > BalanceOperationService | withdrawMoneyByUserId -> Bakiye bilgisi bulunamadi! UserId: {}", currentTime.get(), userId);
            return new MoneyNotAvaibleException("Balance info not found for User ID: " + userId);
        });

        if (currentBalance.compareTo(amount) < 0) {
            log.warn(" ({}) > BalanceOperationService | withdrawMoneyByUserId -> Yetersiz bakiye. UserId: {}, Mevcut: {}, Istened: {}", currentTime.get(), userId, currentBalance, amount);
            throw new MoneyNotAvaibleException("Insufficient funds for User ID: " + userId);
        }

        int result = userMoneyRepository.decrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error(" ({}) > BalanceOperationService | withdrawMoneyByUserId -> Para cekme basarisiz. UserId bulunamadi! {}", currentTime.get(), userId);
            throw new UserNotFoundException("Withdraw failed. User ID not found: " + userId);
        }

        log.info(" ({}) > BalanceOperationService | withdrawMoneyByUserId -> Para cekme basarili. UserId: {}", currentTime.get(), userId);
    }

    @Transactional
    public void withdrawBlockedMoneyByIban(String iban, BigDecimal amount) {
        log.info(" ({}) > BalanceOperationService | withdrawBlockedMoneyByIban -> Blokeli bakiyeden para cekme istegi. IBAN: {}, Miktar: {}", currentTime.get(), iban, amount);

        int result = userMoneyRepository.decrementBlockedByIban(iban, amount);
        if (result == 0) {
            log.error(" ({}) > BalanceOperationService | withdrawBlockedMoneyByIban -> Blokeli bakiyeden para cekme basarisiz (Bakiye yetersiz veya IBAN hatali)! IBAN: {}", currentTime.get(), iban);
            throw new MoneyNotAvaibleException("Blocked withdraw failed. IBAN not found or insufficient blocked funds: " + iban);
        }

        log.info(" ({}) > BalanceOperationService | withdrawBlockedMoneyByIban -> Blokeli bakiyeden para cekme basarili. IBAN: {}", currentTime.get(), iban);
    }
}
