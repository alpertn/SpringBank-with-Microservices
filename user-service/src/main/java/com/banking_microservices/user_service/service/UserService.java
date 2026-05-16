package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import com.banking_microservices.user_service.dto.user.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.dto.enums.TransactionStatus;
import com.banking_microservices.user_service.dto.enums.TransactionType;
import com.banking_microservices.user_service.exception.EmailChangeException;
import com.banking_microservices.user_service.exception.InvalidPasswordException;
import com.banking_microservices.user_service.exception.LoginException;
import com.banking_microservices.user_service.exception.UserAlreadyExistsException;
import com.banking_microservices.user_service.exception.UserNameOrSurnameNotFoundException;
import com.banking_microservices.user_service.exception.UserNotFoundById;
import com.banking_microservices.user_service.exception.UserNotFoundByName;
import com.banking_microservices.user_service.exception.UserRoleUpdateException;
import com.banking_microservices.user_service.exception.UserUpdateException;
import com.banking_microservices.user_service.kafka.KafkaSender;
import com.banking_microservices.user_service.models.Users;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
@Slf4j
public class UserService {

    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakUserService keycloakUserService;
    private final KafkaSender kafkaSender;
    private final Supplier<String> currentTime;

    public UserService(KeycloakAdminService keycloakAdminService,
                       KeycloakUserService keycloakUserService,
                       KafkaSender kafkaSender,
                       Supplier<String> currentTime) {
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakUserService = keycloakUserService;
        this.kafkaSender = kafkaSender;
        this.currentTime = currentTime;
    }

    public List<Users> getAllUsers() {
        return keycloakAdminService.listAllUsers();
    }

    public long countByRole(Role role) {
        return getAllUsers().stream().filter(user -> role == user.getRole()).count();
    }

    public long countByActive(boolean active) {
        return getAllUsers().stream().filter(user -> active == Boolean.TRUE.equals(user.getActive())).count();
    }

    public Users findUserByMail(String mail) {
        return keycloakAdminService.findByEmail(mail);
    }

    public void transactionTopicMessageVerify(KafkaTransactionTopicMessageDto dto) {
        try {
            Users senderUser = findByKeycloakUUID(dto.getSenderUserId());

            dto.setSenderName(senderUser.getName());
            dto.setSenderSurname(senderUser.getSurname());
            dto.setSenderEmail(senderUser.getMail());

            if (dto.getTransactionType() == TransactionType.TRANSFER) {
                Users receiverUser = keycloakAdminService.findExactByNameAndSurname(dto.getReceiverName(), dto.getReceiverSurname())
                        .orElseThrow(() -> new UserNameOrSurnameNotFoundException("Receiver Name/Surname Not Found"));

                dto.setReceiverUserId(receiverUser.getKeycloakUUID());
                dto.setReceiverEmail(receiverUser.getMail());
            }

            dto.setUserValidation(true);
            dto.setStatus(TransactionStatus.VALIDATION_PENDING);
            dto.setStatusDescription(TransactionStatus.VALIDATION_PENDING.getDescription());
            kafkaSender.sendTransactionUserValidationSuccess(dto.getEventUUID(), dto);
        } catch (UserNotFoundById | UserNameOrSurnameNotFoundException exception) {
            dto.setError(true);
            dto.setErrorDescription("Username not found or ID mismatch. " + exception.getMessage());
            dto.setStatus(TransactionStatus.FAILED);
            dto.setStatusDescription(TransactionStatus.FAILED.getDescription());
            kafkaSender.sendTransactionUsernameValidationError(dto.getEventUUID(), dto);
            throw exception;
        } catch (Exception exception) {
            dto.setError(true);
            dto.setErrorDescription("Unexpected validation error. " + exception.getMessage());
            dto.setStatus(TransactionStatus.FAILED);
            dto.setStatusDescription(TransactionStatus.FAILED.getDescription());
            kafkaSender.sendTransactionUsernameValidationError(dto.getEventUUID(), dto);
            throw new UserUpdateException("Unexpected validation failure: " + exception.getMessage());
        }
    }

    public Users findUserById(String id) {
        return keycloakAdminService.findById(id);
    }

    public Users findByKeycloakUUID(String keycloakUUID) {
        return keycloakAdminService.findByKeycloakUUID(keycloakUUID);
    }

    public void updateUser(Users user) {
        try {
            keycloakAdminService.updateProfile(user);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UserUpdateException("An Error With Update User. User : \n" + gson.toJson(user));
        }
    }

    public void deleteUserById(String id) {
        keycloakAdminService.deleteUserById(id);
    }

    public void updateUserRole(String id, Role newRole) {
        try {
            keycloakAdminService.updateUserRole(id, newRole);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UserRoleUpdateException("Kullanici rolu guncellenemedi: " + id);
        }
    }

    public List<Users> searchUsersByName(String name) {
        List<Users> results = keycloakAdminService.searchByName(name);
        if (results.isEmpty()) {
            throw new UserNotFoundByName("Not Found By User Name: " + name);
        }
        return results;
    }

    public long getTotalUserCount() {
        return getAllUsers().size();
    }

    public void updateUserStatus(String id, Boolean active) {
        keycloakAdminService.updateUserStatus(id, Boolean.TRUE.equals(active));
        log.info(" ({}) > UserService | updateUserStatus -> ID: {}, active: {}", currentTime.get(), id, active);
    }

    public List<Users> searchUsersByEmail(String email) {
        List<Users> results = keycloakAdminService.searchByEmail(email);
        if (results.isEmpty()) {
            throw new UserNotFoundByName("No users found with email containing: " + email);
        }
        return results;
    }

    public void resetUserPassword(String id, String newPassword) {
        keycloakAdminService.resetPassword(id, newPassword);
        log.warn(" ({}) > UserService | resetUserPassword -> Admin password reset for user ID: {}", currentTime.get(), id);
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        Users user = findUserById(userId);

        if (currentPassword.equals(newPassword)) {
            throw new InvalidPasswordException("New password cannot be the same as current password");
        }

        try {
            keycloakUserService.verifyCredentials(user.getMail(), currentPassword);
            keycloakAdminService.resetPassword(userId, newPassword);
        } catch (LoginException exception) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
    }

    public void changeEmail(String userId, String newEmail, String password) {
        Users user = findUserById(userId);

        if (user.getMail().equalsIgnoreCase(newEmail)) {
            throw new EmailChangeException("New email cannot be the same as current email");
        }
        if (keycloakAdminService.existsByEmail(newEmail)) {
            throw new UserAlreadyExistsException("Email already in use: " + newEmail);
        }

        try {
            keycloakUserService.verifyCredentials(user.getMail(), password);
            user.setMail(newEmail);
            keycloakAdminService.updateProfile(user);
        } catch (UserAlreadyExistsException | EmailChangeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EmailChangeException("Failed to change email for user: " + userId);
        }
    }
}
