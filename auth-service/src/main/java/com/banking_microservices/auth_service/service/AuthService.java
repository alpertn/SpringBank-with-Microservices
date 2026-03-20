package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.CreateUserTopicDto;
import com.banking_microservices.auth_service.dto.RegisterDto;
import com.banking_microservices.auth_service.dto.Role;
import com.banking_microservices.auth_service.exception.KeycloackUserCreateException;
import com.banking_microservices.auth_service.kafka.KafkaSender;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 *  Bu Class {@link KeycloackAdminService}, {@link KeycloackUserService} ve {@link KafkaSender} Classlarini cagirir.
 *
 *  Create User istegini Controller aldiginda bu classi cagirir. Tum createuser islemi bu class uzerinden yonetilir.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

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
    private final KeycloackAdminService keycloackAdminService;
    private final KeycloackUserService keycloackUserService;
    private final KafkaSender kafkaSender;

    /**
     * 1 - {@link KeycloackAdminService} Çağırarak Keycloack User olusturur Role.User Enum turuyle. ve keycloackUserUUID degıskenını alır.
     * 2 - keycloackUserUUID degiskeni ve RegisterDto turundeki veriyi CreateUserTopicDto classina cevirir
     * 3 - {@link KafkaSender} ile Kafkaya Veriyi Gonderir
     * @param dto Controller registerDto turundeki veriyi input gonderir.
     * @throws KeycloackUserCreateException turunde exception firlatir. GlobalExceptionHandling ile gozukur.
     *
     */
    public void createUser(RegisterDto dto) {
        log.info(" ({}) > AuthService | createUser -> Metoda veri geldi. {}", currentTime.get(), gson.toJson(dto));

        try {

            String keycloackUserUUID = keycloackAdminService.createKeycloakUser(dto, Role.USER);
            log.info(" ({}) > AuthService | createUser -> Keycloak kullanicisi olusturuldu. UUID: {}", currentTime.get(), keycloackUserUUID);

            CreateUserTopicDto userTopicDto = CreateUserTopicDto.builder()
                    .keycloackUserUUID(keycloackUserUUID)
                    .name(dto.getName())
                    .surname(dto.getSurname())
                    .password(dto.getPassword())
                    .email(dto.getEmail()).build();

            log.info(" ({}) > AuthService | createUser -> Kafkaya veri gonderiliyor. {}", currentTime.get(), gson.toJson(userTopicDto));
            kafkaSender.sendCreateUserToUserTopic(userTopicDto);

        } catch (Exception e) {
            log.error(" ({}) > AuthService | createUser -> Keycloak kullanicisi olusturulamadi! Hata: {}", currentTime.get(), e);
            throw new KeycloackUserCreateException("An exception with Keycloack create user: " + e);
        }

    }

}
