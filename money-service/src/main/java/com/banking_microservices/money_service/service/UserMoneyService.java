package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.MoneyDto;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.models.UserMoney;
import com.banking_microservices.money_service.repository.UserMoneyRepository;

import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class UserMoneyService {

    private final UserMoneyRepository UserMoneyRepository;

    public UserMoneyService(UserMoneyRepository UserMoneyRepository) {
        this.UserMoneyRepository = UserMoneyRepository;
    }

    @Transactional
    public MoneyDto generateUser(String userId) {
        if (UserMoneyRepository.existsByUserId(userId)) {
            log.warn("Kullanici icin IBAN zaten mevcut. ID: {}", userId);
            throw new SaveUserException("This user already has an IBAN");
        }
        UserMoney newMoney = UserMoney.builder()
                .userId(userId)
                .userIban(generateRandomTurkishIban())
                .money(BigDecimal.ZERO)
                .blockedMoney(BigDecimal.ZERO)
                .build();
        UserMoney savedMoney = UserMoneyRepository.save(newMoney);
        log.info("Kullanici icin yeni hesap olusturuldu. {}", savedMoney);
        return mapToDto(savedMoney);
    }

    public String generateRandomTurkishIban() {
        String iban;
        do {
            iban = Iban.random(CountryCode.TR).toString();
        } while (UserMoneyRepository.existsByUserIban(iban));

        log.debug("Yeni IBAN üretildi: {}", iban);
        return iban;
    }

    public MoneyDto getAccountById(String id) {
        log.info("getAccountById isteği. ID: {}", id);
        UserMoney userMoney = UserMoneyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. ID: {}", id);
                    return new UserNotFoundException("Account not found for ID: " + id);
                });
        return mapToDto(userMoney);
    }

    public MoneyDto getAccountByIban(String iban) {
        log.info("getAccountByIban isteği. IBAN: {}", iban);
        UserMoney userMoney = UserMoneyRepository.findByUserIban(iban)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. IBAN: {}", iban);
                    return new UserNotFoundException("Account not found for IBAN: " + iban);
                });
        return mapToDto(userMoney);
    }

    public MoneyDto getAccountByUserId(String userId) {
        log.info("getAccountByUserId isteği. UserId: {}", userId);
        UserMoney userMoney = UserMoneyRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. UserId: {}", userId);
                    return new UserNotFoundException("Account not found for User ID: " + userId);
                });
        return mapToDto(userMoney);
    }

    public BigDecimal getBalanceById(String id) {
        log.info("getBalanceById isteği. ID: {}", id);
        return UserMoneyRepository.findBalanceById(id)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. ID: {}", id);
                    return new UserNotFoundException("Balance info not found for ID: " + id);
                });
    }

    public BigDecimal getBalanceByIban(String iban) {
        log.info("getBalanceByIban isteği. IBAN: {}", iban);
        return UserMoneyRepository.findBalanceByIban(iban)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. IBAN: {}", iban);
                    return new UserNotFoundException("Balance info not found for IBAN: " + iban);
                });
    }

    public BigDecimal getBalanceByUserId(String userId) {
        log.info("getBalanceByUserId isteği. UserId: {}", userId);
        return UserMoneyRepository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. UserId: {}", userId);
                    return new UserNotFoundException("Balance info not found for User ID: " + userId);
                });
    }

    @Transactional
    public void depositMoneyById(String id, BigDecimal amount) {
        log.info("depositMoneyById isteği. ID: {}, Miktar: {}", id, amount);
        int result = UserMoneyRepository.incrementBalanceById(id, amount);
        if (result == 0) {
            log.error("Para yatırma başarısız. ID bulunamadı: {}", id);
            throw new DepositFailedException("Deposit failed. ID not found: " + id);
        }
        log.info("Para yatırma başarılı. ID: {}", id);
    }

    @Transactional
    public void withdrawMoneyById(String id, BigDecimal amount) {
        log.info("withdrawMoneyById isteği. ID: {}, Miktar: {}", id, amount);
        BigDecimal currentBalance = getBalanceById(id);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Yetersiz bakiye. ID: {}, Mevcut: {}, İstenen: {}", id, currentBalance,
                    amount);
            throw new MoneyNotAvaibleException("Insufficient funds for ID: " + id);
        }
        int result = UserMoneyRepository.decrementBalanceById(id, amount);
        if (result == 0) {
            log.error("Para çekme başarısız. ID bulunamadı: {}", id);
            throw new UserNotFoundException("Withdraw failed. ID not found: " + id);
        }
        log.info("Para çekme başarılı. ID: {}", id);
    }

    @Transactional
    public void depositMoneyByIban(String iban, BigDecimal amount) {
        log.info("depositMoneyByIban isteği. IBAN: {}, Miktar: {}", iban, amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        int result = UserMoneyRepository.incrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error("Para yatırma başarısız. IBAN bulunamadı: {}", iban);
            throw new IbanNotFoundException("Deposit failed. IBAN not found: " + iban);
        }
        log.info("Para yatırma başarılı. IBAN: {}", iban);
    }

    @Transactional
    public void withdrawMoneyByIban(String iban, BigDecimal amount) {
        log.info("withdrawMoneyByIban isteği. IBAN: {}, Miktar: {}", iban, amount);
        BigDecimal currentBalance = getBalanceByIban(iban);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Yetersiz bakiye. IBAN: {}, Mevcut: {}, İstenen: {}", iban,
                    currentBalance, amount);
            throw new MoneyNotAvaibleException("Insufficient funds for IBAN: " + iban);
        }

        int result = UserMoneyRepository.decrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error("Para çekme başarısız. IBAN bulunamadı: {}", iban);
            throw new IbanNotFoundException("Withdraw failed. IBAN not found: " + iban);
        }
        log.info("Para çekme başarılı. IBAN: {}", iban);
    }

    @Transactional
    public void depositMoneyByUserId(String userId, BigDecimal amount) {
        log.info("depositMoneyByUserId isteği. UserId: {}, Miktar: {}", userId, amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegativeNumberException("Deposit amount must be positive");
        }
        int result = UserMoneyRepository.incrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error("Para yatırma başarısız. UserId bulunamadı: {}", userId);
            throw new UserNotFoundException("Deposit failed. User ID not found: " + userId);
        }
        log.info("Para yatırma başarılı. UserId: {}", userId);
    }

    @Transactional
    public void withdrawMoneyByUserId(String userId, BigDecimal amount) {
        log.info("withdrawMoneyByUserId isteği. UserId: {}, Miktar: {}", userId, amount);
        BigDecimal currentBalance = UserMoneyRepository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. UserId: {}", userId);
                    return new MoneyNotAvaibleException("Balance info not found for User ID: " + userId);
                });

        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Yetersiz bakiye. UserId: {}, Mevcut: {}, İstenen: {}", userId,
                    currentBalance, amount);
            throw new MoneyNotAvaibleException("Insufficient funds for User ID: " + userId);
        }

        int result = UserMoneyRepository.decrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error("Para çekme başarısız. UserId bulunamadı: {}", userId);
            throw new UserNotFoundException("Withdraw failed. User ID not found: " + userId);
        }
        log.info("Para çekme başarılı. UserId: {}", userId);
    }

    private MoneyDto mapToDto(UserMoney userMoney) {
        return MoneyDto.builder()
                .id(userMoney.getId())
                .userId(userMoney.getUserId())
                .userIban(userMoney.getUserIban())
                .money(userMoney.getMoney())
                .blockedMoney(userMoney.getBlockedMoney())
                .build();
    }
}