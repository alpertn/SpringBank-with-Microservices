package com.banking_microservices.money_service.repository;

import com.banking_microservices.money_service.models.Money;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface repository extends JpaRepository<Money, String> {

    @Query("SELECT m.money FROM Money m WHERE m.userIban = :iban")
    Optional<BigDecimal> findBalanceByIban(@Param("iban") String iban);

    @Query("SELECT m.userId FROM Money m WHERE m.userIban = :iban")
    Optional<String> findUserIdByIban(@Param("iban") String iban);

    @Query("SELECT m.id FROM Money m WHERE m.userIban = :iban")
    Optional<String> findIdByIban(@Param("iban") String iban);

    Optional<Money> findByUserIban(String iban);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = :newBalance WHERE m.userIban = :iban")
    int updateBalanceByIban(@Param("iban") String iban, @Param("newBalance") BigDecimal newBalance);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = m.money + :amount WHERE m.userIban = :iban")
    int incrementBalanceByIban(@Param("iban") String iban, @Param("amount") BigDecimal amount);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = m.money - :amount WHERE m.userIban = :iban")
    int decrementBalanceByIban(@Param("iban") String iban, @Param("amount") BigDecimal amount);

    @Query("SELECT m.money FROM Money m WHERE m.userId = :userId")
    Optional<BigDecimal> findBalanceByUserId(@Param("userId") String userId);

    @Query("SELECT m.userIban FROM Money m WHERE m.userId = :userId")
    Optional<String> findIbanByUserId(@Param("userId") String userId);

    @Query("SELECT m.id FROM Money m WHERE m.userId = :userId")
    Optional<String> findIdByUserId(@Param("userId") String userId);

    Optional<Money> findByUserId(String userId);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = :newBalance WHERE m.userId = :userId")
    int updateBalanceByUserId(@Param("userId") String userId, @Param("newBalance") BigDecimal newBalance);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = m.money + :amount WHERE m.userId = :userId")
    int incrementBalanceByUserId(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = m.money - :amount WHERE m.userId = :userId")
    int decrementBalanceByUserId(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    @Query("SELECT m.money FROM Money m WHERE m.id = :id")
    Optional<BigDecimal> findBalanceById(@Param("id") String id);

    @Query("SELECT m.userId FROM Money m WHERE m.id = :id")
    Optional<String> findUserIdById(@Param("id") String id);

    @Query("SELECT m.userIban FROM Money m WHERE m.id = :id")
    Optional<String> findIbanById(@Param("id") String id);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = :newBalance WHERE m.id = :id")
    int updateBalanceById(@Param("id") String id, @Param("newBalance") BigDecimal newBalance);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = m.money + :amount WHERE m.id = :id")
    int incrementBalanceById(@Param("id") String id, @Param("amount") BigDecimal amount);

    @Transactional
    @Modifying
    @Query("UPDATE Money m SET m.money = m.money - :amount WHERE m.id = :id")
    int decrementBalanceById(@Param("id") String id, @Param("amount") BigDecimal amount);

    boolean existsByUserIban(String iban);

    boolean existsByUserId(String userId);
    
}
