package com.springbank.user_service.repository;

import com.springbank.user_service.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface repository extends JpaRepository<Users, String>{

    Optional<Users> getUsersByIban(String iban);

    Optional<Users> getUsersByTckimlikNo(String tckimlikNo);

    Optional<Users> getUsersByPassword(String password);

    Optional<Users> getUsersByCustomerNameAndCustomerSurname(String customerName, String customerSurname);

    Optional<Users> getUsersById(String id);
}