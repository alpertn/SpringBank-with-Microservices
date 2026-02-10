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

    void deleteUsersById(String id);

    List<Users> findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(String name, String surname);

    long countByRole(String role);

    // Role göre kullanıcıları bul
    List<Users> findByRole(String role);

    // Email ile arama (kısmi eşleşme)
    List<Users> findByMailContainingIgnoreCase(String email);

    // Gelişmiş arama için custom query
    @Query("SELECT u FROM Users u WHERE " +
            "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:surname IS NULL OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :surname, '%'))) AND " +
            "(:email IS NULL OR LOWER(u.mail) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:active IS NULL OR u.active = :active)")
    List<Users> advancedSearch(@Param("name") String name,
            @Param("surname") String surname,
            @Param("email") String email,
            @Param("role") String role,
            @Param("active") Boolean active);
}
