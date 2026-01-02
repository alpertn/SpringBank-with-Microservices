package com.springbank.user_service.repository;

import com.springbank.user_service.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface repository extends JpaRepository<Users, String>{

    Optional<Users> getUserByIban(String iban);

}