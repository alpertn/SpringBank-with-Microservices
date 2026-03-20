package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.RegisterDto;
import com.banking_microservices.auth_service.dto.Role;
import com.banking_microservices.auth_service.exception.KeycloackUserCreateException;
import com.banking_microservices.auth_service.exception.KeycloakUserAlreadyExists;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloackAdminService {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    private final Supplier<String> currentTime;

    public KeycloackAdminService(Supplier<String> currentTime) {
        this.currentTime = currentTime;
    }

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    private Keycloak keycloak;

    // keycloack config
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

    public String createKeycloakUser(RegisterDto register, Role role) {
        log.info(" ({}) > KeycloackAdminService | createKeycloakUser -> Metoda veri geldi. DTO: {}, Role: {}", currentTime.get(), gson.toJson(register), role.name());

        if (existsByEmail(register.getEmail())) {
            log.warn(" ({}) > KeycloackAdminService | createKeycloakUser -> Kullanici zaten var! DTO: {}", currentTime.get(), gson.toJson(register));
            throw new KeycloakUserAlreadyExists("Email Already Exists");
        }
        // Ceredential (Password Bigileri icin keycloakda zorunludur)
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(register.getPassword());
        credential.setTemporary(false);

        // User
        UserRepresentation user = new UserRepresentation();
        user.setEmail(register.getEmail());
        user.setUsername(register.getEmail());
        user.setFirstName(register.getName());
        user.setLastName(register.getSurname());
        user.setEnabled(true);
        user.setCredentials(Collections.singletonList(credential));

        log.info(" ({}) > KeycloackAdminService | createKeycloakUser -> Keycloak'a kullanici olusturma istegi atiliyor.", currentTime.get());
        // jakartanin response kullaniyoruz keycloaka ait degil realmina istek gonderip
        // responseyi aliyor
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() != 201) {
            String errorInfo = response.readEntity(String.class);
            response.close();
            log.error(" ({}) > KeycloackAdminService | createKeycloakUser -> Keycloak'da kullanici olusturulamadi! Status: {}, Info: {}", currentTime.get(), response.getStatus(), errorInfo);
            throw new KeycloackUserCreateException("Keycloak user creation failed: " + errorInfo);
        }

        // Responseden User Id cekme.
        String location = response.getHeaderString("Location");
        if (location == null) {
            response.close();
            log.error(" ({}) > KeycloackAdminService | createKeycloakUser -> UserId alinamadi: Location header eksik!", currentTime.get());
            throw new KeycloackUserCreateException("Location header missing after successful creation.");
        }

        String userId = location.substring(location.lastIndexOf('/') + 1);
        // http istegi acik kaldigi icin kapatmamiz lazim.
        response.close();
        
        log.info(" ({}) > KeycloackAdminService | createKeycloakUser -> Kullanici olusturuldu. UserId: {}", currentTime.get(), userId);

        assignRole(userId, role.name());

        return userId;

    }

    private void assignRole(String userId, String roleName) {
        log.info(" ({}) > KeycloackAdminService | assignRole -> Role atama islemi basladi. UserId: {}, Role: {}", currentTime.get(), userId, roleName);
        try {
            // try catch blogu ile http istegi otomatik kapandigi icin kapatmamzia gerek
            // kalmiyor
            var role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
            log.info(" ({}) > KeycloackAdminService | assignRole -> Role atamasi basarili.", currentTime.get());
        } catch (Exception e) {
            log.error(" ({}) > KeycloackAdminService | assignRole -> Role atamasinda hata olustu! Hata: {}", currentTime.get(), e);
            throw new KeycloackUserCreateException("An exception with assign role in keycloak");
        }
    }

    public boolean existsByEmail(String email) {
        List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
        boolean exists = users != null && !users.isEmpty();
        log.info(" ({}) > KeycloackAdminService | existsByEmail -> Email kontrolu: {} - Mevcut mu? {}", currentTime.get(), email, exists);
        return exists;
    }

}