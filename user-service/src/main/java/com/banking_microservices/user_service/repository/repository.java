package com.banking_microservices.user_service.repository;

import com.banking_microservices.user_service.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface repository extends JpaRepository<Users, String>{

    Optional<Users> findUserById(String id);

    Optional<Users> getUsersByNameAndSurname(String customerName, String customerSurname);
    
    boolean existsBymail(String email);

    Optional<Users> findUsersByMail(String mail);
}
