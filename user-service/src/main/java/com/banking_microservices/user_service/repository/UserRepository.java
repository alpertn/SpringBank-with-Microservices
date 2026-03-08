package com.banking_microservices.user_service.repository;

import com.banking_microservices.user_service.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, String> {

    Optional<Users> findUsersById(String id);

    Optional<Users> getUsersByNameAndSurname(String customerName, String customerSurname);

    boolean existsBymail(String email);

    Optional<Users> findUsersByMail(String mail);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Users u WHERE UPPER(TRIM(u.name)) = UPPER(TRIM(:name)) AND UPPER(TRIM(u.surname)) = UPPER(TRIM(:surname))")
    boolean existsByNameAndSurname(@Param("name") String name, @Param("surname") String surname);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Users u WHERE u.id = :id AND UPPER(TRIM(u.name)) = UPPER(TRIM(:name)) AND UPPER(TRIM(u.surname)) = UPPER(TRIM(:surname))")
    boolean existsByIdAndNameAndSurname(@Param("id") String id, @Param("name") String name,
            @Param("surname") String surname);

    void deleteUsersById(String id);

    List<Users> findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(String name, String surname);

    long countByRole(String role);

    List<Users> findByRole(String role);

    List<Users> findByMailContainingIgnoreCase(String email);

}
