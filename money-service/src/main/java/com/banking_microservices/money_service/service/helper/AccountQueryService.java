package com.banking_microservices.money_service.service.helper;

import com.banking_microservices.money_service.dto.MoneyDto;
import com.banking_microservices.money_service.exception.UserNotFoundException;
import com.banking_microservices.money_service.models.UserMoney;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Bu class {@link UserMoneyRepository} classini cagirir.
 *
 * Hesap ve bakiye sorgulama islemlerini yonetir.
 * Bulunamazsa {@link UserNotFoundException} firlatir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountQueryService {

    private final UserMoneyRepository userMoneyRepository;
    private final Supplier<String> currentTime;

    /**
     * Document ID ile hesap bilgisini doner.
     *
     * @param id document id
     * @return {@link MoneyDto} hesap bilgisi
     * @throws UserNotFoundException hesap bulunamazsa firlatir
     */
    public MoneyDto getAccountById(String id) {
        log.info(" ({}) > AccountQueryService | getAccountById -> Metoda veri geldi. ID: {}", currentTime.get(), id);
        UserMoney userMoney = userMoneyRepository.findById(id).orElseThrow(() -> {
            log.warn(" ({}) > AccountQueryService | getAccountById -> Hesap bulunamadi! ID: {}", currentTime.get(), id);
            return new UserNotFoundException("Account not found for ID: " + id);
        });
        return mapToDto(userMoney);
    }

    /**
     * IBAN ile hesap bilgisini doner.
     *
     * @param iban kullanici iban kodu
     * @return {@link MoneyDto} hesap bilgisi
     * @throws UserNotFoundException hesap bulunamazsa firlatir
     */
    public MoneyDto getAccountByIban(String iban) {
        log.info(" ({}) > AccountQueryService | getAccountByIban -> Metoda veri geldi. IBAN: {}", currentTime.get(), iban);
        UserMoney userMoney = userMoneyRepository.findByUserIban(iban).orElseThrow(() -> {
            log.warn(" ({}) > AccountQueryService | getAccountByIban -> Hesap bulunamadi! IBAN: {}", currentTime.get(), iban);
            return new UserNotFoundException("Account not found for IBAN: " + iban);
        });
        return mapToDto(userMoney);
    }

    /**
     * userId ile hesap bilgisini doner.
     *
     * @param userId keycloak uuid
     * @return {@link MoneyDto}
     * @throws UserNotFoundException hesap bulunamazsa firlatir
     */
    public MoneyDto getAccountByUserId(String userId) {
        log.info(" ({}) > AccountQueryService | getAccountByUserId -> Metoda veri geldi. UserId: {}", currentTime.get(), userId);
        UserMoney userMoney = userMoneyRepository.findByUserId(userId).orElseThrow(() -> {
            log.warn(" ({}) > AccountQueryService | getAccountByUserId -> Hesap bulunamadi! UserId: {}", currentTime.get(), userId);
            return new UserNotFoundException("Account not found for User ID: " + userId);
        });
        return mapToDto(userMoney);
    }

    /**
     * Document ID ile bakiye doner.
     *
     * @param id document id
     * @return bakiye {@link BigDecimal}
     * @throws UserNotFoundException hesap bulunamazsa firlatir
     */
    public BigDecimal getBalanceById(String id) {
        log.info(" ({}) > AccountQueryService | getBalanceById -> Metoda veri geldi. ID: {}", currentTime.get(), id);
        return userMoneyRepository.findBalanceById(id).orElseThrow(() -> {
            log.warn(" ({}) > AccountQueryService | getBalanceById -> Bakiye bilgisi bulunamadi! ID: {}", currentTime.get(), id);
            return new UserNotFoundException("Balance info not found for ID: " + id);
        });
    }

    /**
     * IBAN ile bakiye doner.
     *
     * @param iban kullanici iban kodu
     * @return bakiye {@link BigDecimal}
     * @throws UserNotFoundException hesap bulunamazsa firlatir
     */
    public BigDecimal getBalanceByIban(String iban) {
        log.info(" ({}) > AccountQueryService | getBalanceByIban -> Metoda veri geldi. IBAN: {}", currentTime.get(), iban);
        return userMoneyRepository.findBalanceByIban(iban).orElseThrow(() -> {
            log.warn(" ({}) > AccountQueryService | getBalanceByIban -> Bakiye bilgisi bulunamadi! IBAN: {}", currentTime.get(), iban);
            return new UserNotFoundException("Balance info not found for IBAN: " + iban);
        });
    }

    /**
     * userId ile bakiye doner.
     *
     * @param userId keycloak uuid
     * @return bakiye {@link BigDecimal}
     * @throws UserNotFoundException hesap bulunamazsa firlatir
     */
    public BigDecimal getBalanceByUserId(String userId) {
        log.info(" ({}) > AccountQueryService | getBalanceByUserId -> Metoda veri geldi. UserId: {}", currentTime.get(), userId);
        return userMoneyRepository.findBalanceByUserId(userId).orElseThrow(() -> {
            log.warn(" ({}) > AccountQueryService | getBalanceByUserId -> Bakiye bilgisi bulunamadi! UserId: {}", currentTime.get(), userId);
            return new UserNotFoundException("Balance info not found for User ID: " + userId);
        });
    }

    // dto donusumu icin yardimci method
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
