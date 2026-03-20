package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.MoneyDto;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.models.UserMoney;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Service
@Slf4j
public class UserMoneyService {

    private final UserMoneyRepository UserMoneyRepository;
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    private final Supplier<String> currentTime;

    public UserMoneyService(UserMoneyRepository UserMoneyRepository, Supplier<String> currentTime) {
        this.UserMoneyRepository = UserMoneyRepository;
        this.currentTime = currentTime;
    }

    public UserMoney generateUser(String userId) {
        log.info(" ({}) > UserMoneyService | generateUser -> generateUser istegi alindi. UserId: {}", currentTime.get(), gson.toJson(userId));
        try {
            UserMoney userMoney = UserMoney.builder()
                    .userId(userId)
                    .userIban(generateRandomTurkishIban())
                    .build();
            try {
                UserMoney savedUserMoney = UserMoneyRepository.save(userMoney);
                log.info(" ({}) > UserMoneyService | generateUser -> Kullanici UserMoney servisine kaydedildi. {}", currentTime.get(), gson.toJson(savedUserMoney));
                return savedUserMoney;
            } catch (Exception e) {
                log.error(" ({}) > UserMoneyService | generateUser -> Veritabanina kayit sirasinda hata olustu! UserId: {}, Hata: {}", currentTime.get(), gson.toJson(userId), e.getMessage());
                throw new SaveUserException("Failed to save user on UserMoney-Service " + userMoney.getUserId());
            }
        } catch (Exception e) {
            log.error(" ({}) > UserMoneyService | generateUser -> generateUser metodunda beklenmeyen hata! UserId: {}, Hata: {}", currentTime.get(), gson.toJson(userId), e.getMessage());
            throw new SaveUserException(
                    "Failed Save User in money-UserMoneyService " + userId + " details " + e.getMessage());
        }
    }

    public String generateRandomTurkishIban() {
        String iban;
        do {
            iban = Iban.random(CountryCode.TR).toString();
        } while (UserMoneyRepository.existsByUserIban(iban));

        log.debug(" ({}) > UserMoneyService | generateRandomTurkishIban -> Yeni IBAN uretildi. {}", currentTime.get(), gson.toJson(iban));
        return iban;
    }

    public MoneyDto getAccountById(String id) {
        log.info(" ({}) > UserMoneyService | getAccountById -> Metoda veri geldi. ID: {}", currentTime.get(), gson.toJson(id));
        UserMoney userMoney = UserMoneyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn(" ({}) > UserMoneyService | getAccountById -> Hesap bulunamadi! ID: {}", currentTime.get(), gson.toJson(id));
                    return new UserNotFoundException("Account not found for ID: " + id);
                });
        return mapToDto(userMoney);
    }

    public MoneyDto getAccountByIban(String iban) {
        log.info(" ({}) > UserMoneyService | getAccountByIban -> Metoda veri geldi. IBAN: {}", currentTime.get(), gson.toJson(iban));
        UserMoney userMoney = UserMoneyRepository.findByUserIban(iban)
                .orElseThrow(() -> {
                    log.warn(" ({}) > UserMoneyService | getAccountByIban -> Hesap bulunamadi! IBAN: {}", currentTime.get(), gson.toJson(iban));
                    return new UserNotFoundException("Account not found for IBAN: " + iban);
                });
        return mapToDto(userMoney);
    }

    public MoneyDto getAccountByUserId(String userId) {
        log.info(" ({}) > UserMoneyService | getAccountByUserId -> Metoda veri geldi. UserId: {}", currentTime.get(), gson.toJson(userId));
        UserMoney userMoney = UserMoneyRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn(" ({}) > UserMoneyService | getAccountByUserId -> Hesap bulunamadi! UserId: {}", currentTime.get(), gson.toJson(userId));
                    return new UserNotFoundException("Account not found for User ID: " + userId);
                });
        return mapToDto(userMoney);
    }

    public BigDecimal getBalanceById(String id) {
        log.info(" ({}) > UserMoneyService | getBalanceById -> Metoda veri geldi. ID: {}", currentTime.get(), gson.toJson(id));
        return UserMoneyRepository.findBalanceById(id)
                .orElseThrow(() -> {
                    log.warn(" ({}) > UserMoneyService | getBalanceById -> Bakiye bilgisi bulunamadi! ID: {}", currentTime.get(), gson.toJson(id));
                    return new UserNotFoundException("Balance info not found for ID: " + id);
                });
    }

    public BigDecimal getBalanceByIban(String iban) {
        log.info(" ({}) > UserMoneyService | getBalanceByIban -> Metoda veri geldi. IBAN: {}", currentTime.get(), gson.toJson(iban));
        return UserMoneyRepository.findBalanceByIban(iban)
                .orElseThrow(() -> {
                    log.warn(" ({}) > UserMoneyService | getBalanceByIban -> Bakiye bilgisi bulunamadi! IBAN: {}", currentTime.get(), gson.toJson(iban));
                    return new UserNotFoundException("Balance info not found for IBAN: " + iban);
                });
    }

    public BigDecimal getBalanceByUserId(String userId) {
        log.info(" ({}) > UserMoneyService | getBalanceByUserId -> Metoda veri geldi. UserId: {}", currentTime.get(), gson.toJson(userId));
        return UserMoneyRepository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn(" ({}) > UserMoneyService | getBalanceByUserId -> Bakiye bilgisi bulunamadi! UserId: {}", currentTime.get(), gson.toJson(userId));
                    return new UserNotFoundException("Balance info not found for User ID: " + userId);
                });
    }

    @Transactional
    public void depositMoneyById(String id, BigDecimal amount) {
        log.info(" ({}) > UserMoneyService | depositMoneyById -> Para yatirma istegi. ID: {}, Miktar: {}", currentTime.get(), gson.toJson(id), gson.toJson(amount));
        int result = UserMoneyRepository.incrementBalanceById(id, amount);
        if (result == 0) {
            log.error(" ({}) > UserMoneyService | depositMoneyById -> Para yatirma basarisiz. ID bulunamadi! {}", currentTime.get(), gson.toJson(id));
            throw new DepositFailedException("Deposit failed. ID not found: " + id);
        }
        log.info(" ({}) > UserMoneyService | depositMoneyById -> Para yatirma basarili. ID: {}", currentTime.get(), gson.toJson(id));
    }

    @Transactional
    public void withdrawMoneyById(String id, BigDecimal amount) {
        log.info(" ({}) > UserMoneyService | withdrawMoneyById -> Para cekme istegi. ID: {}, Miktar: {}", currentTime.get(), gson.toJson(id), gson.toJson(amount));
        BigDecimal currentBalance = getBalanceById(id);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn(" ({}) > UserMoneyService | withdrawMoneyById -> Yetersiz bakiye. ID: {}, Mevcut: {}, Istenen: {}", currentTime.get(), gson.toJson(id), gson.toJson(currentBalance), gson.toJson(amount));
            throw new MoneyNotAvaibleException("Insufficient funds for ID: " + id);
        }
        int result = UserMoneyRepository.decrementBalanceById(id, amount);
        if (result == 0) {
            log.error(" ({}) > UserMoneyService | withdrawMoneyById -> Para cekme basarisiz. ID bulunamadi! {}", currentTime.get(), gson.toJson(id));
            throw new UserNotFoundException("Withdraw failed. ID not found: " + id);
        }
        log.info(" ({}) > UserMoneyService | withdrawMoneyById -> Para cekme basarili. ID: {}", currentTime.get(), gson.toJson(id));
    }

    @Transactional
    public void depositMoneyByIban(String iban, BigDecimal amount) {
        log.info(" ({}) > UserMoneyService | depositMoneyByIban -> Para yatirma istegi. IBAN: {}, Miktar: {}", currentTime.get(), gson.toJson(iban), gson.toJson(amount));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        int result = UserMoneyRepository.incrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error(" ({}) > UserMoneyService | depositMoneyByIban -> Para yatirma basarisiz. IBAN bulunamadi! {}", currentTime.get(), gson.toJson(iban));
            throw new IbanNotFoundException("Deposit failed. IBAN not found: " + iban);
        }
        log.info(" ({}) > UserMoneyService | depositMoneyByIban -> Para yatirma basarili. IBAN: {}", currentTime.get(), gson.toJson(iban));
    }

    @Transactional
    public void withdrawMoneyByIban(String iban, BigDecimal amount) {
        log.info(" ({}) > UserMoneyService | withdrawMoneyByIban -> Para cekme istegi. IBAN: {}, Miktar: {}", currentTime.get(), gson.toJson(iban), gson.toJson(amount));
        BigDecimal currentBalance = getBalanceByIban(iban);
        if (currentBalance.compareTo(amount) < 0) {
            log.warn(" ({}) > UserMoneyService | withdrawMoneyByIban -> Yetersiz bakiye. IBAN: {}, Mevcut: {}, Istenen: {}", currentTime.get(), gson.toJson(iban), gson.toJson(currentBalance), gson.toJson(amount));
            throw new MoneyNotAvaibleException("Insufficient funds for IBAN: " + iban);
        }

        int result = UserMoneyRepository.decrementBalanceByIban(iban, amount);
        if (result == 0) {
            log.error(" ({}) > UserMoneyService | withdrawMoneyByIban -> Para cekme basarisiz. IBAN bulunamadi! {}", currentTime.get(), gson.toJson(iban));
            throw new IbanNotFoundException("Withdraw failed. IBAN not found: " + iban);
        }
        log.info(" ({}) > UserMoneyService | withdrawMoneyByIban -> Para cekme basarili. IBAN: {}", currentTime.get(), gson.toJson(iban));
    }

    @Transactional
    public void depositMoneyByUserId(String userId, BigDecimal amount) {
        log.info(" ({}) > UserMoneyService | depositMoneyByUserId -> Para yatirma istegi. UserId: {}, Miktar: {}", currentTime.get(), gson.toJson(userId), gson.toJson(amount));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegativeNumberException("Deposit amount must be positive");
        }
        int result = UserMoneyRepository.incrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error(" ({}) > UserMoneyService | depositMoneyByUserId -> Para yatirma basarisiz. UserId bulunamadi! {}", currentTime.get(), gson.toJson(userId));
            throw new UserNotFoundException("Deposit failed. User ID not found: " + userId);
        }
        log.info(" ({}) > UserMoneyService | depositMoneyByUserId -> Para yatirma basarili. UserId: {}", currentTime.get(), gson.toJson(userId));
    }

    @Transactional
    public void withdrawMoneyByUserId(String userId, BigDecimal amount) {
        log.info(" ({}) > UserMoneyService | withdrawMoneyByUserId -> Para cekme istegi. UserId: {}, Miktar: {}", currentTime.get(), gson.toJson(userId), gson.toJson(amount));
        BigDecimal currentBalance = UserMoneyRepository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn(" ({}) > UserMoneyService | withdrawMoneyByUserId -> Bakiye bilgisi bulunamadi! UserId: {}", currentTime.get(), gson.toJson(userId));
                    return new MoneyNotAvaibleException("Balance info not found for User ID: " + userId);
                });

        if (currentBalance.compareTo(amount) < 0) {
            log.warn(" ({}) > UserMoneyService | withdrawMoneyByUserId -> Yetersiz bakiye. UserId: {}, Mevcut: {}, Istenen: {}", currentTime.get(), gson.toJson(userId), gson.toJson(currentBalance), gson.toJson(amount));
            throw new MoneyNotAvaibleException("Insufficient funds for User ID: " + userId);
        }

        int result = UserMoneyRepository.decrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error(" ({}) > UserMoneyService | withdrawMoneyByUserId -> Para cekme basarisiz. UserId bulunamadi! {}", currentTime.get(), gson.toJson(userId));
            throw new UserNotFoundException("Withdraw failed. User ID not found: " + userId);
        }
        log.info(" ({}) > UserMoneyService | withdrawMoneyByUserId -> Para cekme basarili. UserId: {}", currentTime.get(), gson.toJson(userId));
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