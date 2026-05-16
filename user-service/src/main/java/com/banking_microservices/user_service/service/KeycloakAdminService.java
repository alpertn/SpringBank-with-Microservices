package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import com.banking_microservices.user_service.dto.auth.RegisterDto;
import com.banking_microservices.user_service.exception.KeycloakAssignRoleException;
import com.banking_microservices.user_service.exception.KeycloakConnectionException;
import com.banking_microservices.user_service.exception.KeycloakUserAlreadyExists;
import com.banking_microservices.user_service.exception.UserNotFoundById;
import com.banking_microservices.user_service.models.Users;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Slf4j
public class KeycloakAdminService {

    private static final String ROLE_ATTRIBUTE = "app_role";

    private final Supplier<String> currentTime;

    public KeycloakAdminService(Supplier<String> currentTime) {
        this.currentTime = currentTime;
    }

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    private Keycloak keycloak;

    @PostConstruct
    public void init() {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm("master")
                .username(adminUsername)
                .password(adminPassword)
                .clientId("admin-cli")
                .build();
    }

    public String createUser(RegisterDto registerDto, Role role) {
        if (existsByEmail(registerDto.getEmail())) {
            throw new KeycloakUserAlreadyExists("Email Already Exists");
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(registerDto.getPassword());
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setEmail(registerDto.getEmail());
        user.setUsername(registerDto.getEmail());
        user.setFirstName(registerDto.getName());
        user.setLastName(registerDto.getSurname());
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCredentials(Collections.singletonList(credential));
        user.setAttributes(Map.of(ROLE_ATTRIBUTE, List.of(role.name())));

        Response response = keycloak.realm(realm).users().create(user);
        if (response.getStatus() != 201) {
            String errorInfo = response.readEntity(String.class);
            response.close();
            throw new KeycloakConnectionException("Keycloak user creation failed: " + errorInfo);
        }

        String location = response.getHeaderString("Location");
        response.close();
        if (location == null) {
            throw new KeycloakConnectionException("Location header missing after successful creation.");
        }

        String userId = location.substring(location.lastIndexOf('/') + 1);
        updateUserRole(userId, role);
        log.info(" ({}) > KeycloakAdminService | createUser -> User created: {}", currentTime.get(), userId);
        return userId;
    }

    public boolean existsByEmail(String email) {
        return !keycloak.realm(realm).users().searchByEmail(email, true).isEmpty();
    }

    public Users findById(String id) {
        UserRepresentation user = keycloak.realm(realm).users().get(id).toRepresentation();
        if (user == null || user.getId() == null) {
            throw new UserNotFoundById("User Not Found By Id: " + id);
        }
        return toUserModel(user);
    }

    public Users findByKeycloakUUID(String keycloakUUID) {
        return findById(keycloakUUID);
    }

    public Users findByEmail(String email) {
        return keycloak.realm(realm).users().searchByEmail(email, true).stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .map(this::toUserModel)
                .orElseThrow(() -> new UserNotFoundById("User not found by email: " + email));
    }

    public List<Users> searchByEmail(String email) {
        return keycloak.realm(realm).users().searchByEmail(email, false).stream()
                .map(this::toUserModel)
                .toList();
    }

    public List<Users> searchByName(String query) {
        String normalized = normalize(query);
        return listAllUsers().stream()
                .filter(user -> normalize(user.getName()).contains(normalized)
                        || normalize(user.getSurname()).contains(normalized))
                .toList();
    }

    public Optional<Users> findExactByNameAndSurname(String name, String surname) {
        String normalizedName = normalize(name);
        String normalizedSurname = normalize(surname);

        return listAllUsers().stream()
                .filter(user -> normalize(user.getName()).equals(normalizedName)
                        && normalize(user.getSurname()).equals(normalizedSurname))
                .findFirst();
    }

    public List<Users> listAllUsers() {
        List<Users> users = new ArrayList<>();
        int first = 0;
        int size = 100;

        while (true) {
            List<UserRepresentation> page = keycloak.realm(realm).users().list(first, size);
            if (page == null || page.isEmpty()) {
                break;
            }
            page.stream().map(this::toUserModel).forEach(users::add);
            if (page.size() < size) {
                break;
            }
            first += size;
        }
        return users;
    }

    public void updateProfile(Users user) {
        String userId = resolveUserId(user);
        UserRepresentation existing = keycloak.realm(realm).users().get(userId).toRepresentation();
        if (existing == null || existing.getId() == null) {
            throw new UserNotFoundById("User Not Found: " + userId);
        }
        if (!Objects.equals(existing.getEmail(), user.getMail()) && existsByEmail(user.getMail())) {
            throw new KeycloakUserAlreadyExists("Email already in use: " + user.getMail());
        }

        existing.setEmail(user.getMail());
        existing.setUsername(user.getMail());
        existing.setFirstName(user.getName());
        existing.setLastName(user.getSurname());
        existing.setEnabled(Boolean.TRUE.equals(user.getActive()));
        keycloak.realm(realm).users().get(userId).update(existing);

        if (user.getRole() != null) {
            updateUserRole(userId, user.getRole());
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            resetPassword(userId, user.getPassword());
        }
    }

    public void updateUserStatus(String id, boolean active) {
        UserRepresentation user = keycloak.realm(realm).users().get(id).toRepresentation();
        if (user == null || user.getId() == null) {
            throw new UserNotFoundById("User Not Found: " + id);
        }
        user.setEnabled(active);
        keycloak.realm(realm).users().get(id).update(user);
    }

    public void deleteUserById(String id) {
        try {
            keycloak.realm(realm).users().get(id).remove();
        } catch (Exception exception) {
            throw new KeycloakConnectionException("User delete failed: " + exception.getMessage());
        }
    }

    public void resetPassword(String id, String newPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);
        keycloak.realm(realm).users().get(id).resetPassword(credential);
    }

    public void updateUserRole(String id, Role newRole) {
        try {
            var userResource = keycloak.realm(realm).users().get(id);
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
            List<RoleRepresentation> removableRoles = currentRoles.stream()
                    .filter(role -> "USER".equalsIgnoreCase(role.getName()) || "ADMIN".equalsIgnoreCase(role.getName()))
                    .toList();
            if (!removableRoles.isEmpty()) {
                userResource.roles().realmLevel().remove(removableRoles);
            }

            RoleRepresentation targetRole = keycloak.realm(realm).roles().get(newRole.name()).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(targetRole));

            UserRepresentation userRepresentation = userResource.toRepresentation();
            userRepresentation.setAttributes(Map.of(ROLE_ATTRIBUTE, List.of(newRole.name())));
            userResource.update(userRepresentation);
        } catch (Exception exception) {
            throw new KeycloakAssignRoleException("Kullanici rolu guncellenemedi: " + id);
        }
    }

    private Users toUserModel(UserRepresentation representation) {
        return Users.builder()
                .id(representation.getId())
                .keycloakUUID(representation.getId())
                .mail(representation.getEmail())
                .password("")
                .name(defaultString(representation.getFirstName()))
                .surname(defaultString(representation.getLastName()))
                .role(resolveRole(representation))
                .active(Boolean.TRUE.equals(representation.isEnabled()))
                .createdAt(toLocalDateTime(representation.getCreatedTimestamp()))
                .build();
    }

    private Role resolveRole(UserRepresentation representation) {
        Map<String, List<String>> attributes = representation.getAttributes();
        if (attributes != null && attributes.containsKey(ROLE_ATTRIBUTE) && !attributes.get(ROLE_ATTRIBUTE).isEmpty()) {
            return Role.valueOf(attributes.get(ROLE_ATTRIBUTE).getFirst().toUpperCase());
        }

        List<RoleRepresentation> roles = keycloak.realm(realm)
                .users()
                .get(representation.getId())
                .roles()
                .realmLevel()
                .listEffective();

        return roles.stream()
                .map(RoleRepresentation::getName)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .filter(name -> "ADMIN".equals(name) || "USER".equals(name))
                .map(Role::valueOf)
                .findFirst()
                .orElse(Role.USER);
    }

    private LocalDateTime toLocalDateTime(Long createdTimestamp) {
        if (createdTimestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(createdTimestamp), ZoneId.systemDefault());
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String resolveUserId(Users user) {
        if (user.getId() != null && !user.getId().isBlank()) {
            return user.getId();
        }
        if (user.getKeycloakUUID() != null && !user.getKeycloakUUID().isBlank()) {
            return user.getKeycloakUUID();
        }
        throw new UserNotFoundById("User id/keycloakUUID is required");
    }
}
