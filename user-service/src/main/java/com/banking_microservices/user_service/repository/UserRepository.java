package com.banking_microservices.user_service.repository;

import com.banking_microservices.user_service.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, String>{

    Optional<Users> findUserById(String id);

    Optional<Users> getUsersByNameAndSurname(String customerName, String customerSurname);
    
    boolean existsBymail(String email);

    Optional<Users> findUsersByMail(String mail);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Users u WHERE UPPER(TRIM(u.name)) = UPPER(TRIM(:name)) AND UPPER(TRIM(u.surname)) = UPPER(TRIM(:surname))")
    boolean existsByNameAndSurname(@Param("name") String name, @Param("surname") String surname);
}
