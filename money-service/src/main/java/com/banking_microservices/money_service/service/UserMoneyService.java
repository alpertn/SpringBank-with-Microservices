package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.MoneyDto;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.models.UserMoney;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.google.gson.Gson;
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
    private final Gson gson;

    public UserMoneyService(UserMoneyRepository UserMoneyRepository, Gson gson) {
        this.UserMoneyRepository = UserMoneyRepository;
        this.gson = gson;
    }


    @Transactional
    public UserMoney generateUser(String userId){
        log.info("generateUser isteği alındı. UserId: {}", gson.toJson(userId));
        try{
            UserMoney userMoney = UserMoney.builder()
                    .userId(userId)
                    .userIban(generateRandomTurkishIban())
                    .build();
            try{
                UserMoney savedUserMoney = UserMoneyRepository.save(userMoney);
                log.info("Kullanıcı UserMoney servisine kaydedildi: {}", gson.toJson(savedUserMoney));
                return savedUserMoney;
            }catch (Exception e){
                log.error("Veritabanına kayıt sırasında hata oluştu. UserId: {}", gson.toJson(userId), e);
                throw new SaveUserException("Failed to save user on UserMoney-Service " + userMoney.getUserId());
            }
        }catch(Exception e){
            log.error("generateUser metodunda beklenmeyen hata. UserId: {}", gson.toJson(userId), e);
            throw new SaveUserException("Failed Save User in money-UserMoneyService " + userId + " details " + e.getMessage());
        }
    }

    public String generateRandomTurkishIban() {
        String iban;
        do{
            iban = Iban.random(CountryCode.TR).toString();
        } while(UserMoneyRepository.existsByUserIban(iban));

        log.debug("Yeni IBAN üretildi: {}", gson.toJson(iban));
        return iban;
    }

    public MoneyDto getAccountById(String id) {
        log.info("getAccountById isteği. ID: {}", gson.toJson(id));
        UserMoney userMoney = UserMoneyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. ID: {}", gson.toJson(id));
                    return new UserNotFoundException("Account not found for ID: " + id);
                });
        return mapToDto(userMoney);
    }

    public MoneyDto getAccountByIban(String iban) {
        log.info("getAccountByIban isteği. IBAN: {}", gson.toJson(iban));
        UserMoney userMoney = UserMoneyRepository.findByUserIban(iban)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. IBAN: {}", gson.toJson(iban));
                    return new UserNotFoundException("Account not found for IBAN: " + iban);
                });
        return mapToDto(userMoney);
    }

    public MoneyDto getAccountByUserId(String userId) {
        log.info("getAccountByUserId isteği. UserId: {}", gson.toJson(userId));
        UserMoney userMoney = UserMoneyRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Hesap bulunamadı. UserId: {}", gson.toJson(userId));
                    return new UserNotFoundException("Account not found for User ID: " + userId);
                });
        return mapToDto(userMoney);
    }

    public BigDecimal getBalanceById(String id) {
        log.info("getBalanceById isteği. ID: {}", gson.toJson(id));
        return UserMoneyRepository.findBalanceById(id)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. ID: {}", gson.toJson(id));
                    return new UserNotFoundException("Balance info not found for ID: " + id);
                });
    }

    public BigDecimal getBalanceByIban(String iban) {
        log.info("getBalanceByIban isteği. IBAN: {}", gson.toJson(iban));
        return UserMoneyRepository.findBalanceByIban(iban)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. IBAN: {}", gson.toJson(iban));
                    return new UserNotFoundException("Balance info not found for IBAN: " + iban);
                });
    }

    public BigDecimal getBalanceByUserId(String userId) {
        log.info("getBalanceByUserId isteği. UserId: {}", gson.toJson(userId));
        return UserMoneyRepository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. UserId: {}", gson.toJson(userId));
                    return new UserNotFoundException("Balance info not found for User ID: " + userId);
                });
    }

    @Transactional
    public void depositMoneyById(String id, BigDecimal amount) {
        log.info("depositMoneyById isteği. ID: {}, Miktar: {}", gson.toJson(id), gson.toJson(amount));
        int result = UserMoneyRepository.incrementBalanceById(id, amount);
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
        int result = UserMoneyRepository.decrementBalanceById(id, amount);
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
        int result = UserMoneyRepository.incrementBalanceByIban(iban, amount);
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

        int result = UserMoneyRepository.decrementBalanceByIban(iban, amount);
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
        int result = UserMoneyRepository.incrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error("Para yatırma başarısız. UserId bulunamadı: {}", gson.toJson(userId));
            throw new UserNotFoundException("Deposit failed. User ID not found: " + userId);
        }
        log.info("Para yatırma başarılı. UserId: {}", gson.toJson(userId));
    }

    @Transactional
    public void withdrawMoneyByUserId(String userId, BigDecimal amount) {
        log.info("withdrawMoneyByUserId isteği. UserId: {}, Miktar: {}", gson.toJson(userId), gson.toJson(amount));
        BigDecimal currentBalance = UserMoneyRepository.findBalanceByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Bakiye bilgisi bulunamadı. UserId: {}", gson.toJson(userId));
                    return new MoneyNotAvaibleException("Balance info not found for User ID: " + userId);
                });

        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Yetersiz bakiye. UserId: {}, Mevcut: {}, İstenen: {}", gson.toJson(userId), gson.toJson(currentBalance), gson.toJson(amount));
            throw new MoneyNotAvaibleException("Insufficient funds for User ID: " + userId);
        }

        int result = UserMoneyRepository.decrementBalanceByUserId(userId, amount);
        if (result == 0) {
            log.error("Para çekme başarısız. UserId bulunamadı: {}", gson.toJson(userId));
            throw new UserNotFoundException("Withdraw failed. User ID not found: " + userId);
        }
        log.info("Para çekme başarılı. UserId: {}", gson.toJson(userId));
    }

    private MoneyDto mapToDto(UserMoney userMoney) {
        return MoneyDto.builder()
                .id(userMoney.getId())
                .userId(userMoney.getUserId())
                .userIban(userMoney.getUserIban())
                .money(userMoney.getMoney())
                .build();
    }
}