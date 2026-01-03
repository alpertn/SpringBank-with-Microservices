package com.springbank.money_service.repository;

import com.springbank.money_service.dto.moneyDto;
import com.springbank.money_service.models.money;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface repository extends JpaRepository<money, String> {

    @Modifying
    @Transactional
    @Query("UPDATE money a SET a.balance = a.balance - :amount WHERE a.iban = :iban")
    int deleteMoneyByIban(@Param("iban") String iban, @Param("amount") Double amount);

    @Modifying
    @Transactional
    @Query("UPDATE money a SET a.balance = a.balance WHERE a.iban = :iban")
    int updateMoneyByIban(@Param("iban") String iban, @Param("amount") Double amount);

    @Modifying
    @Transactional
    @Query("UPDATE money a SET a.balance = a.balance + :amount WHERE a.iban = :iban")
    int addMoneyByIban(@Param("iban") String iban, @Param("amount") Double amount);


    Optional<money> findMoneyByIban(String iban);
}
