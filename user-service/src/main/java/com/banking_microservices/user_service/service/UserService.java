package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.client.MoneyServiceClient;
import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import com.banking_microservices.user_service.dto.user.AuthServiceCreateUserTopicDto;
import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.dto.user.UsersDto;
import com.banking_microservices.user_service.exception.*;
import com.banking_microservices.user_service.kafka.KafkaSender;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private Gson gson = new Gson();
    @Autowired
    private UserRepository UserRepository;
    private MoneyServiceClient moneyServiceClient;
    private final KafkaSender kafkaSender;

    public UserService(UserRepository UserRepository, MoneyServiceClient moneyServiceClient, KafkaSender kafkaSender) {
        this.UserRepository = UserRepository;
        this.moneyServiceClient = moneyServiceClient;
        this.kafkaSender = kafkaSender;
    }

    @Transactional
    public List<Users> getAllUsers() {
        return UserRepository.findAll();
    }

    @Transactional
    public Users findUserByMail(String mail) {
        return UserRepository.findUsersByMail(mail)
                .orElseThrow(() -> new MailNotFoundException("Mail Not Found: {}" + mail));
    }

    public Users saveUser(AuthServiceCreateUserTopicDto authServiceCreateUserTopicDto) {

        if (UserRepository.existsBymail(authServiceCreateUserTopicDto.getEmail())) {
            throw new UserAlreadyExistsException("Mail Already Exists " + authServiceCreateUserTopicDto.getEmail());
        }
        try {
            Role role = Role.valueOf(String.valueOf(authServiceCreateUserTopicDto.getRole()));
            Users newUsers = Users
                    .builder()
                    .mail(authServiceCreateUserTopicDto.getEmail())
                    .password(authServiceCreateUserTopicDto.getPassword())
                    .name(authServiceCreateUserTopicDto.getName())
                    .surname(authServiceCreateUserTopicDto.getSurname())
                    .keycloackUUID(authServiceCreateUserTopicDto.getKeycloackUserUUID())
                    .role(Role.valueOf(role.name()))
                    .build();
            try {
                Users user = UserRepository.save(newUsers);
                log.info("User Olusturuldu! {}", gson.toJson(user));
                try {

                    kafkaSender.sendCreateUser(user.getId());
                    return newUsers;

                } catch (Exception e) {

                    throw new KafkaSendException("Kafka ile Create user topicine mesaj gonderilirken hata olustu.");

                }

            } catch (Exception e) {
                throw new UserSaveDatabaseException(
                        "User veritabanina kaydedilirken bir sorun olustu " + e.getMessage() + gson.toJson(newUsers));
            }

        } catch (IllegalArgumentException e) {
            throw new RoleParseException("An error with parse role" + e.getMessage());
        }

    }

    //
    // Transaction Topic Message
    //

    public void transactionTopicMessageVerify(KafkaTransactionTopicMessageDto dto) {
        if (UserRepository.existsByIdAndNameAndSurname(dto.getReceiverUserId(), dto.getName(), dto.getSurname())) {
            dto.setUserValidation(true);
            kafkaSender.sendTransactionUserValidationSuccess(dto.getEventUUID(), dto);
        } else {
            dto.setError(true);
            dto.setErrorDescription("Username not found or ID mismatch. " + dto.getName() + " " + dto.getSurname());
            kafkaSender.sendTransactionUsernameValidationError(dto.getEventUUID(), dto);
            throw new UserNameOrSurnameNotFoundException(
                    "User Name Or Surname Not Found or ID mismatch " + dto.getName() + " " + dto.getSurname());
        }
    }

    public void UsernameValidation(KafkaTransactionTopicMessageDto dto) {
        try {
            UserRepository.existsByNameAndSurname(dto.getName(), dto.getSurname());
            kafkaSender.sendUsernameValidationSuccess(dto.getEventUUID(), dto);
        } catch (Exception e) {
            kafkaSender.sendUsernameValidationError(dto.getEventUUID(), dto);
            throw new UserNameOrSurnameNotFoundException(
                    "User Name Or Surname Not Found " + dto.getName() + " " + dto.getSurname());
        }
    }

    public Users findUserById(String id) {
        return UserRepository.findUsersById(String.valueOf(id))
                .orElseThrow(() -> new UserNotFoundById("User Not Found By Id: {}" + id));

    }

    public void updateUser(Users user) {
        try {
            UserRepository.save(user);
        } catch (Exception e) {
            throw new UserUpdateException("An Error With Update User. User : {}" + gson.toJson(user));
        }
    }

    @Transactional
    public void deleteUserById(String id) {
        if (!UserRepository.existsById(String.valueOf(id))) {
            throw new UserNotFoundById("User Not Found for delete: " + id);
        }

        try {
            UserRepository.deleteUsersById(id);
        } catch (Exception e) {
            throw new DeleteUserException("An Error With delete user. id: " + id);
        }

    }

    @Transactional
    public void updateUserRole(String id, Role newRole) {
        Users user = UserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + id));

        try {
            user.setRole(Role.valueOf(newRole.name()));
            UserRepository.save(user);
        } catch (Exception e) {
            throw new UserRoleUpdateException("Kullanici rolu guncellenemedi: " + id);
        }
    }

    public List<Users> searchUsersByName(String name) {
        List<Users> results = UserRepository.findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(name, name);

        if (results.isEmpty()) {
            throw new UserNotFoundByName("Not Found By User Name: " + name);
        }
        return results;
    }

    public long getTotalUserCount() {
        return UserRepository.count();
    }

    // 2. Kullanıcı Aktif/Pasif Yapma
    @Transactional
    public void updateUserStatus(String id, Boolean active) {
        Users user = UserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + id));

        try {
            user.setActive(active);
            UserRepository.save(user);
            log.info("User status updated. ID: {}, Active: {}", id, active);
        } catch (Exception e) {
            throw new UserUpdateException("Failed to update user status: " + id);
        }
    }

    // Email ile Kullanıcı Arama
    public List<Users> searchUsersByEmail(String email) {
        List<Users> results = UserRepository.findByMailContainingIgnoreCase(email);
        if (results.isEmpty()) {
            throw new UserNotFoundByName("No users found with email containing: " + email);
        }
        return results;
    }

    // Kullanıcı Şifre Sıfırlama (Admin)
    @Transactional
    public void resetUserPassword(String id, String newPassword) {
        Users user = UserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + id));

        try {
            user.setPassword(newPassword);
            UserRepository.save(user);
            log.warn("Admin password reset for user ID: {}", id);
        } catch (Exception e) {
            throw new UserUpdateException("Failed to reset password for user: " + id);
        }
    }

    // Kullanıcı Şifre Değiştirme (Kendisi)
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        Users user = UserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + userId));

        // Mevcut şifreyi doğrula
        if (!user.getPassword().equals(currentPassword)) {
            log.warn("Password change failed for user ID: {} - incorrect current password", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Yeni şifre eski şifre ile aynı olamaz
        if (currentPassword.equals(newPassword)) {
            throw new InvalidPasswordException("New password cannot be the same as current password");
        }

        try {
            user.setPassword(newPassword);
            UserRepository.save(user);
            log.info("Password changed successfully for user ID: {}", userId);
        } catch (Exception e) {
            throw new UserUpdateException("Failed to change password for user: " + userId);
        }
    }

    // Kullanıcı Email Değiştirme
    @Transactional
    public void changeEmail(String userId, String newEmail, String password) {
        Users user = UserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + userId));

        // Şifreyi doğrula
        if (!user.getPassword().equals(password)) {
            log.warn("Email change failed for user ID: {} - incorrect password", userId);
            throw new InvalidPasswordException("Password is incorrect");
        }

        // Yeni email zaten kullanılıyor mu kontrol et
        if (UserRepository.existsBymail(newEmail)) {
            log.warn("Email change failed for user ID: {} - email already exists: {}", userId, newEmail);
            throw new UserAlreadyExistsException("Email already in use: " + newEmail);
        }

        // Yeni email eski email ile aynı olamaz
        if (user.getMail().equals(newEmail)) {
            throw new EmailChangeException("New email cannot be the same as current email");
        }

        try {
            String oldEmail = user.getMail();
            user.setMail(newEmail);
            UserRepository.save(user);
            log.info("Email changed successfully for user ID: {} from {} to {}", userId, oldEmail, newEmail);
        } catch (Exception e) {
            throw new EmailChangeException("Failed to change email for user: " + userId);
        }
    }

}
