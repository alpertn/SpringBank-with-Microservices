package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.MoneyDto;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.models.Money;
import com.banking_microservices.money_service.repository.repository;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class service {

    private final repository repository;
    private final Gson gson;

    public service(repository repository, Gson gson) {
        this.repository = repository;
        this.gson = gson;
    }

    @Transactional
    public Money generateUser(String userId){
        log.info("generateUser isteği alındı. UserId: {}", gson.toJson(userId));
        try{
            Money userMoney = Money.builder()
                    .userId(userId)
                    .userIban(generateRandomTurkishIban())
                    .build();
            try{
                Money savedMoney = repository.save(userMoney);
                log.info("Kullanıcı Money servisine kaydedildi: {}", gson.toJson(savedMoney));
                return savedMoney;
            }catch (Exception e){
                log.error("Veritabanına kayıt sırasında hata oluştu. UserId: {}", gson.toJson(userId), e);
                throw new SaveUserException("Failed to save user on Money-Service " + userMoney.getUserId());
            }
        }catch(Exception e){
            log.error("generateUser metodunda beklenmeyen hata. UserId: {}", gson.toJson(userId), e);
            throw new SaveUserException("Failed Save User in money-service " + userId + " details " + e.getMessage());
        }
    }

    public String generateRandomTurkishIban() {
        String iban;
        do{
            iban = Iban.random(CountryCode.TR).toString();
        } while(repository.existsByUserIban(iban));

        log.debug("Yeni IBAN üretildi: {}", gson.toJson(iban));
        return iban;
    }

    public MoneyDto getAccountById(String id) {
        log.info("getAccountById isteği. ID: {}", gson.toJson(id));
        Money money = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. ID: {}", gson.toJson(id));
                    return new UserNotFoundException("Account not found for ID: " + id);
                });
        return mapToDto(money);
    }

    public MoneyDto getAccountByIban(String iban) {
        log.info("getAccountByIban isteği. IBAN: {}", gson.toJson(iban));
        Money money = repository.findByUserIban(iban)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. IBAN: {}", gson.toJson(iban));
                    return new UserNotFoundException("Account not found for IBAN: " + iban);
                });
        return mapToDto(money);
    }

    public MoneyDto getAccountByUserId(String userId) {
        log.info("getAccountByUserId isteği. UserId: {}", gson.toJson(userId));
        Money money = repository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. UserId: {}", gson.toJson(userId));
                    return new UserNotFoundException("Account not found for User ID: " + userId);
                });
        return mapToDto(money);
    }

    public BigDecimal getBalanceById(String id) {
        log.info("getBalanceById isteği. ID: {}", gson.toJson(id));
        return repository.findBalanceById(id)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. ID: {}", gson.toJson(id));
                    return new UserNotFoundException("Balance info not found for ID: " + id);
                });
    }

    public BigDecimal getBalanceByIban(String iban) {
        log.info("getBalanceByIban isteği. IBAN: {}", gson.toJson(iban));
        return repository.findBalanceByIban(iban)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. IBAN: {}", gson.toJson(iban));
                    return new UserNotFoundException("Balance info not found for IBAN: " + iban);
                });
    }

    public BigDecimal getBalanceByUserId(String userId) {
        log.info("getBalanceByUserId isteği. UserId: {}", gson.toJson(userId));
        return repository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. UserId: {}", gson.toJson(userId));
                    return new UserNotFoundException("Balance info not found for User ID: " + userId);
                });
    }

    @Transactional
    public void depositMoneyById(String id, BigDecimal amount) {
        log.info("depositMoneyById isteği. ID: {}, Miktar: {}", gson.toJson(id), gson.toJson(amount));
        int result = repository.incrementBalanceById(id, amount);
        if (result == 0) {
            log.error("Para yatırma başarısız. ID bulunamadı: {}", gson.toJson(id));
            throw new DepositFailedException("Deposit failed. ID not found: " + id);
        }
        log.info("Para yatırma başarılı. ID: {}", gson.toJson(id));
    }

    @Transactional
    public void withdrawMoneyById(String id, BigDecimal amount) {
        log.info("withdrawMoneyById isteği. ID: {}, Miktar: {}", gson.toJson(id), gson.toJson(amount));
        BigDecimal currentBalance = getBalanceById(id);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Yetersiz bakiye. ID: {}, Mevcut: {}, İstenen: {}", gson.toJson(id), gson.toJson(currentBalance), gson.toJson(amount));
            throw new MoneyNotAvaibleException("Insufficient funds for ID: " + id);
        }
        int result = repository.decrementBalanceById(id, amount);
        if (result == 0) {
            log.error("Para çekme başarısız. ID bulunamadı: {}", gson.toJson(id));
            throw new UserNotFoundException("Withdraw failed. ID not found: " + id);
        }
        log.info("Para çekme başarılı. ID: {}", gson.toJson(id));
    }

    @Transactional
    public void depositMoneyByIban(String iban, BigDecimal amount) {
        log.info("depositMoneyByIban isteği. IBAN: {}, Miktar: {}", gson.toJson(iban), gson.toJson(amount));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        int result = repository.incrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error("Para yatırma başarısız. IBAN bulunamadı: {}", gson.toJson(iban));
            throw new IbanNotFoundException("Deposit failed. IBAN not found: " + iban);
        }
        log.info("Para yatırma başarılı. IBAN: {}", gson.toJson(iban));
    }

    @Transactional
    public void withdrawMoneyByIban(String iban, BigDecimal amount) {
        log.info("withdrawMoneyByIban isteği. IBAN: {}, Miktar: {}", gson.toJson(iban), gson.toJson(amount));
        BigDecimal currentBalance = getBalanceByIban(iban);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Yetersiz bakiye. IBAN: {}, Mevcut: {}, İstenen: {}", gson.toJson(iban), gson.toJson(currentBalance), gson.toJson(amount));
            throw new MoneyNotAvaibleException("Insufficient funds for IBAN: " + iban);
        }

        int result = repository.decrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error("Para çekme başarısız. IBAN bulunamadı: {}", gson.toJson(iban));
            throw new IbanNotFoundException("Withdraw failed. IBAN not found: " + iban);
        }
        log.info("Para çekme başarılı. IBAN: {}", gson.toJson(iban));
    }

    @Transactional
    public void depositMoneyByUserId(String userId, BigDecimal amount) {
        log.info("depositMoneyByUserId isteği. UserId: {}, Miktar: {}", gson.toJson(userId), gson.toJson(amount));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegativeNumberException("Deposit amount must be positive");
        }
        int result = repository.incrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error("Para yatırma başarısız. UserId bulunamadı: {}", gson.toJson(userId));
            throw new UserNotFoundException("Deposit failed. User ID not found: " + userId);
        }
        log.info("Para yatırma başarılı. UserId: {}", gson.toJson(userId));
    }

    @Transactional
    public void withdrawMoneyByUserId(String userId, BigDecimal amount) {
        log.info("withdrawMoneyByUserId isteği. UserId: {}, Miktar: {}", gson.toJson(userId), gson.toJson(amount));
        BigDecimal currentBalance = repository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. UserId: {}", gson.toJson(userId));
                    return new MoneyNotAvaibleException("Balance info not found for User ID: " + userId);
                });

        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Yetersiz bakiye. UserId: {}, Mevcut: {}, İstenen: {}", gson.toJson(userId), gson.toJson(currentBalance), gson.toJson(amount));
            throw new MoneyNotAvaibleException("Insufficient funds for User ID: " + userId);
        }

        int result = repository.decrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error("Para çekme başarısız. UserId bulunamadı: {}", gson.toJson(userId));
            throw new UserNotFoundException("Withdraw failed. User ID not found: " + userId);
        }
        log.info("Para çekme başarılı. UserId: {}", gson.toJson(userId));
    }

    private MoneyDto mapToDto(Money money) {
        return MoneyDto.builder()
                .id(money.getId())
                .userId(money.getUserId())
                .userIban(money.getUserIban())
                .money(money.getMoney())
                .build();
    }
}