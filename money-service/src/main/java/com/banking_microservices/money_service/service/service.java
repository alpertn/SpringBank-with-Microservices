package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.MoneyDto;
import com.banking_microservices.money_service.exception.*;
import com.banking_microservices.money_service.models.Money;
import com.banking_microservices.money_service.repository.repository;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class service {

    private final repository repository;

    public service(repository repository) {
        this.repository = repository;
    }

    @Transactional
    public Money generateUser(String userId){
        try{
            Money userMoney = Money.builder()
                    .userId(userId)
                    .userIban(generateRandomTurkishIban())
                    .build();
            try{
                return repository.save(userMoney);
            }catch (Exception e){
                throw new SaveUserException("Failed to save user on Money-Service " + userMoney.getUserId());
            }
        }catch(Exception e){
            throw new SaveUserException("Failed Save User in money-service " + userId + " details " + e.getMessage());
        }
    }

    public String generateRandomTurkishIban() {
        String iban;
        do{
            iban = Iban.random(CountryCode.TR).toString();
        } while(repository.existsByUserIban(iban));

        return iban;
    }

    public MoneyDto getAccountById(String id) {
        Money money = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Account not found for ID: " + id));
        return mapToDto(money);
    }

    public MoneyDto getAccountByIban(String iban) {
        Money money = repository.findByUserIban(iban)
                .orElseThrow(() -> new UserNotFoundException("Account not found for IBAN: " + iban));
        return mapToDto(money);
    }

    public MoneyDto getAccountByUserId(String userId) {
        Money money = repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Account not found for User ID: " + userId));
        return mapToDto(money);
    }

    public BigDecimal getBalanceById(String id) {
        return repository.findBalanceById(id)
                .orElseThrow(() -> new UserNotFoundException("Balance info not found for ID: " + id));
    }

    public BigDecimal getBalanceByIban(String iban) {
        return repository.findBalanceByIban(iban)
                .orElseThrow(() -> new UserNotFoundException("Balance info not found for IBAN: " + iban));
    }

    public BigDecimal getBalanceByUserId(String userId) {
        return repository.findBalanceByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Balance info not found for User ID: " + userId));
    }

    @Transactional
    public void depositMoneyById(String id, BigDecimal amount) {

        int result = repository.incrementBalanceById(id, amount);
        if (result == 0) {
            throw new DepositFailedException("Deposit failed. ID not found: " + id);
        }
    }

    @Transactional
    public void withdrawMoneyById(String id, BigDecimal amount) {
        BigDecimal currentBalance = getBalanceById(id);
        if (currentBalance.compareTo(amount) < 0) {
            throw new MoneyNotAvaibleException("Insufficient funds for ID: " + id);
        }
        int result = repository.decrementBalanceById(id, amount);
        if (result == 0) {
            throw new UserNotFoundException("Withdraw failed. ID not found: " + id);
        }
    }

    @Transactional
    public void depositMoneyByIban(String iban, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        int result = repository.incrementBalanceByIban(iban, amount);
        if (result == 0) {
            throw new IbanNotFoundException("Deposit failed. IBAN not found: " + iban);
        }
    }

    @Transactional
    public void withdrawMoneyByIban(String iban, BigDecimal amount) {
        BigDecimal currentBalance = getBalanceByIban(iban);
        if (currentBalance.compareTo(amount) < 0) {
            throw new MoneyNotAvaibleException("Insufficient funds for IBAN: " + iban);
        }

        int result = repository.decrementBalanceByIban(iban, amount);
        if (result == 0) {
            throw new IbanNotFoundException("Withdraw failed. IBAN not found: " + iban);
        }
    }

    @Transactional
    public void depositMoneyByUserId(String userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegativeNumberException("Deposit amount must be positive");
        }
        int result = repository.incrementBalanceByUserId(userId, amount);
        if (result == 0) {
            throw new UserNotFoundException("Deposit failed. User ID not found: " + userId);
        }
    }

    @Transactional
    public void withdrawMoneyByUserId(String userId, BigDecimal amount) {
        BigDecimal currentBalance = repository.findBalanceByUserId(userId)
                .orElseThrow(() -> new MoneyNotAvaibleException("Balance info not found for User ID: " + userId));

        if (currentBalance.compareTo(amount) < 0) {
            throw new MoneyNotAvaibleException("Insufficient funds for User ID: " + userId);
        }

        int result = repository.decrementBalanceByUserId(userId, amount);
        if (result == 0) {
            throw new UserNotFoundException("Withdraw failed. User ID not found: " + userId);
        }
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