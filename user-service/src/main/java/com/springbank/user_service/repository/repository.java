package com.springbank.user_service.repository;

import com.springbank.user_service.model.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface repository extends JpaRepository<Users, String>{

    Optional<Users> getUsersByIban(String iban);

    Optional<Users> getUsersByTckimlikNo(String tckimlikNo);

    Optional<Users> getUsersByPassword(String password);

    Optional<Users> getUsersByCustomerNameAndCustomerSurname(String customerName, String customerSurname); // iki farki sorgu yapabiliyorsun

    Optional<Users> getUsersById(String id);

    @Modifying
    @Transactional
    @Query("UPDATE Users a SET a.customerBalance = a.customerBalance - :amount WHERE a.iban = :iban")
    int deleteMoneyByIban(@Param("iban") String iban, @Param("amount") Double amount);


}