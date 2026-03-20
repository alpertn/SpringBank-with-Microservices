package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.client.MoneyServiceClient;
import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import com.banking_microservices.user_service.dto.user.AuthServiceCreateUserTopicDto;
import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.dto.enums.TransactionStatus;
import com.banking_microservices.user_service.exception.*;
import com.banking_microservices.user_service.kafka.KafkaSender;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

@Service
@Slf4j
public class UserService {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    private UserRepository UserRepository;
    private MoneyServiceClient moneyServiceClient;
    private final KafkaSender kafkaSender;

    private final java.util.function.Supplier<String> currentTime;

    public UserService(UserRepository UserRepository, MoneyServiceClient moneyServiceClient, KafkaSender kafkaSender, java.util.function.Supplier<String> currentTime) {
        this.UserRepository = UserRepository;
        this.moneyServiceClient = moneyServiceClient;
        this.kafkaSender = kafkaSender;
        this.currentTime = currentTime;
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
                log.info(" ({}) > UserService | saveUser -> User Olusturuldu! Dto: {}", currentTime.get(), gson.toJson(user));
                try {
                    kafkaSender.sendCreateUser(user.getKeycloackUUID());
                    return newUsers;
                } catch (Exception e) {
                    throw new KafkaSendException("Kafka ile Create user topicine mesaj gonderilirken hata olustu.");
                }

            } catch (KafkaSendException e) {
                throw e;
            } catch (Exception e) {
                throw new UserSaveDatabaseException(
                        "User veritabanina kaydedilirken bir sorun olustu " + e.getMessage() + gson.toJson(newUsers));
            }

        } catch (IllegalArgumentException e) {
            throw new RoleParseException("An error with parse role" + e.getMessage());
        }
    }

    public void transactionTopicMessageVerify(KafkaTransactionTopicMessageDto dto) {
        try {
            log.info(" ({}) > UserService | transactionTopicMessageVerify -> Metoda veri geldi. Dto: {}", currentTime.get(), gson.toJson(dto));
            Users senderUser = UserRepository.findUsersBykeycloackUUID(dto.getSenderUserId())
                    .orElseThrow(() -> new UserNotFoundById("Sender Not Found: " + dto.getSenderUserId()));

            dto.setSenderName(senderUser.getName());
            dto.setSenderSurname(senderUser.getSurname());
            dto.setSenderEmail(senderUser.getMail());

            if (dto.getReceiverName() != null && dto.getReceiverSurname() != null) {
                Users receiverUser = UserRepository.getUsersByNameAndSurname(dto.getReceiverName(), dto.getReceiverSurname())
                        .orElseThrow(() -> new UserNameOrSurnameNotFoundException("Receiver Name/Surname Not Found"));

                dto.setReceiverUserId(receiverUser.getKeycloackUUID());
                dto.setReceiverEmail(receiverUser.getMail());
            }

            dto.setUserValidation(true);
            dto.setStatus(TransactionStatus.VALIDATION_PENDING);
            dto.setStatusDescription(TransactionStatus.VALIDATION_PENDING.getDescription());
            log.info(" ({}) > UserService | transactionTopicMessageVerify -> Validation basarili. Kafkaya mesaj atiliyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            kafkaSender.sendTransactionUserValidationSuccess(dto.getEventUUID(), dto);

        } catch (Exception e) {
            log.warn(" ({}) > UserService | transactionTopicMessageVerify -> Validation basarisiz! Hata: {}", currentTime.get(), e.getMessage());
            dto.setError(true);
            dto.setErrorDescription("Username not found or ID mismatch. " + e.getMessage());
            kafkaSender.sendTransactionUsernameValidationError(dto.getEventUUID(), dto);
            throw new UserNameOrSurnameNotFoundException(
                    "User Name Or Surname Not Found or ID mismatch " + e.getMessage());
        }
    }

    public void UsernameValidation(KafkaTransactionTopicMessageDto dto) {
        log.info(" ({}) > UserService | UsernameValidation -> Metoda veri geldi. Dto: {}", currentTime.get(), gson.toJson(dto));
        boolean exists = UserRepository.existsByNameAndSurname(dto.getReceiverName(), dto.getReceiverSurname());
        if (!exists) {
            log.warn(" ({}) > UserService | UsernameValidation -> Kullanici bulunamadi! Dto: {}", currentTime.get(), gson.toJson(dto));
            kafkaSender.sendUsernameValidationError(dto.getEventUUID(), dto);
            throw new UserNameOrSurnameNotFoundException(
                    "User Name Or Surname Not Found " + dto.getReceiverName() + " " + dto.getReceiverSurname());
        }
        try {
            log.info(" ({}) > UserService | UsernameValidation -> Kullanici dogrulandi, kafkaya success mesaji atiliyor. Dto: {}", currentTime.get(), gson.toJson(dto));
            kafkaSender.sendUsernameValidationSuccess(dto.getEventUUID(), dto);
        } catch (Exception e) {
            log.error(" ({}) > UserService | UsernameValidation -> Success mesaji atilamadi! Hata: {}", currentTime.get(), e.getMessage());
            kafkaSender.sendUsernameValidationError(dto.getEventUUID(), dto);
            throw new UserNameOrSurnameNotFoundException(
                    "User Name Or Surname Not Found " + dto.getReceiverName() + " " + dto.getReceiverSurname());
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

    @Transactional
    public void updateUserStatus(String id, Boolean active) {
        Users user = UserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + id));

        try {
            user.setActive(active);
            UserRepository.save(user);
            log.info(" ({}) > UserService | updateUserStatus -> User status updated. ID: {}, Active: {}", currentTime.get(), id, active);
        } catch (Exception e) {
            throw new UserUpdateException("Failed to update user status: " + id);
        }
    }

    public List<Users> searchUsersByEmail(String email) {
        List<Users> results = UserRepository.findByMailContainingIgnoreCase(email);
        if (results.isEmpty()) {
            throw new UserNotFoundByName("No users found with email containing: " + email);
        }
        return results;
    }

    @Transactional
    public void resetUserPassword(String id, String newPassword) {
        Users user = UserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + id));

        try {
            user.setPassword(newPassword);
            UserRepository.save(user);
            log.warn(" ({}) > UserService | resetUserPassword -> Admin password reset for user ID: {}", currentTime.get(), id);
        } catch (Exception e) {
            throw new UserUpdateException("Failed to reset password for user: " + id);
        }
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        Users user = UserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + userId));

        if (!user.getPassword().equals(currentPassword)) {
            log.warn(" ({}) > UserService | changePassword -> Password change failed for user ID: {} - incorrect current password", currentTime.get(), userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        if (currentPassword.equals(newPassword)) {
            throw new InvalidPasswordException("New password cannot be the same as current password");
        }

        try {
            user.setPassword(newPassword);
            UserRepository.save(user);
            log.info(" ({}) > UserService | changePassword -> Password changed successfully for user ID: {}", currentTime.get(), userId);
        } catch (Exception e) {
            throw new UserUpdateException("Failed to change password for user: " + userId);
        }
    }

    @Transactional
    public void changeEmail(String userId, String newEmail, String password) {
        Users user = UserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundById("User Not Found: " + userId));

        if (!user.getPassword().equals(password)) {
            log.warn(" ({}) > UserService | changeEmail -> Email change failed for user ID: {} - incorrect password", currentTime.get(), userId);
            throw new InvalidPasswordException("Password is incorrect");
        }

        if (UserRepository.existsBymail(newEmail)) {
            log.warn(" ({}) > UserService | changeEmail -> Email change failed for user ID: {} - email already exists: {}", currentTime.get(), userId, newEmail);
            throw new UserAlreadyExistsException("Email already in use: " + newEmail);
        }

        if (user.getMail().equals(newEmail)) {
            throw new EmailChangeException("New email cannot be the same as current email");
        }

        try {
            String oldEmail = user.getMail();
            user.setMail(newEmail);
            UserRepository.save(user);
            log.info(" ({}) > UserService | changeEmail -> Email changed successfully for user ID: {} from {} to {}", currentTime.get(), userId, oldEmail, newEmail);
        } catch (Exception e) {
            throw new EmailChangeException("Failed to change email for user: " + userId);
        }
    }
}