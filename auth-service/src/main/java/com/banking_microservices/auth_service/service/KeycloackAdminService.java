package com.banking_microservices.auth_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloackAdminService {


    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;



    public void createKeycloackUser(){
        Map<String, Object> user = Map.of(
                "username", email, // Login'de kullanılacak username
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false // İlk girişte şifre değiştirme zorlamasın
                )));
    }





    @SuppressWarnings("unchecked")
    public Boolean existsByEmail(String email,String adminToken){
        List<Map<String, Object>> users = RestClient.create(keycloakUrl).get()
                .uri("/admin/realms/{realm}/users?email={email}&exact=true", realm, email)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        if (users == null){
            return false;
        }else{
            return true;
        }

    }

    private String getAdminToken(){
        MultiValueMap<String,String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("grant_type", "password");
        requestParams.add("client_id", "admin-cli");
        requestParams.add("username", adminUsername);
        requestParams.add("password", adminPassword);

        Map<String,Object> token = RestClient.create(keycloakUrl).post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(requestParams)
                .retrieve()
                .body(Map.class);

        if(token == null  ||  !token.containsKey("access_token")){
            return null;
        }else{
            return token.get("access_token").toString();
        }

    }

}
//package com.banking_microservices.auth_service.service;
//
//import com.banking_microservices.auth_service.exception.RegistrationFailedException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestClient;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//@Slf4j
//public class KeycloakAdminService {
//
//    @Value("${keycloak.server-url}")
//    private String keycloakUrl;
//
//    @Value("${keycloak.realm}")
//    private String realm;
//
//    @Value("${keycloak.admin-username:admin}")
//    private String adminUsername;
//
//    @Value("${keycloak.admin-password:admin}")
//    private String adminPassword;
//
//    // =======================================
//    // USER OLUŞTUR
//    // 1. Master realm'den admin token al
//    // 2. Email var mı kontrol et
//    // 3. Yoksa Banking realm'de user oluştur
//    // =======================================
//    public void createUser(String email, String password, String fullName) {
//        String token = getAdminToken();
//
//        // Email zaten varsa hata fırlat
//        if (findUserIdByEmail(token, email) != null) {
//            throw new RegistrationFailedException("Bu email zaten kayıtlı: " + email);
//        }
//
//        createKeycloakUser(token, email, password, fullName);
//        log.info("Keycloak user oluşturuldu: {}", email);
//    }
//
//    // =======================================
//    // USER SİL (Rollback için)
//    // =======================================
//    public void deleteUser(String email) {
//        try {
//            String token = getAdminToken();
//            String userId = findUserIdByEmail(token, email);
//
//            if (userId != null) {
//                RestClient.create(keycloakUrl).delete()
//                        .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
//                        .header("Authorization", "Bearer " + token)
//                        .retrieve()
//                        .toBodilessEntity();
//
//                log.info("Keycloak user silindi (rollback): {}", email);
//            }
//        } catch (Exception e) {
//            log.error("Keycloak rollback hatası: {}", email, e);
//        }
//    }
//
//    // =======================================
//    // ŞİFRE GÜNCELLE
//    // =======================================
//    public void updatePassword(String email, String newPassword) {
//        String token = getAdminToken();
//        String userId = findUserIdByEmail(token, email);
//
//        if (userId == null) {
//            throw new RegistrationFailedException("Kullanıcı bulunamadı: " + email);
//        }
//
//        Map<String, Object> credential = Map.of(
//                "type", "password",
//                "value", newPassword,
//                "temporary", false);
//
//        RestClient.create(keycloakUrl).put()
//                .uri("/admin/realms/{realm}/users/{userId}/reset-password", realm, userId)
//                .header("Authorization", "Bearer " + token)
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(credential)
//                .retrieve()
//                .toBodilessEntity();
//
//        log.info("Keycloak şifre güncellendi: {}", email);
//    }
//
//    // =======================================
//    // PRIVATE METHODS
//    // =======================================
//
//    // Master realm'den admin token al
//    private String getAdminToken() {
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("grant_type", "password");
//        params.add("client_id", "admin-cli");
//        params.add("username", adminUsername);
//        params.add("password", adminPassword);
//
//        @SuppressWarnings("unchecked")
//        Map<String, Object> response = RestClient.create(keycloakUrl).post()
//                .uri("/realms/master/protocol/openid-connect/token")
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .body(params)
//                .retrieve()
//                .body(Map.class);
//
//        if (response == null || !response.containsKey("access_token")) {
//            throw new RegistrationFailedException("Keycloak admin token alınamadı");
//        }
//
//        return response.get("access_token").toString();
//    }
//
//    // Banking realm'de user oluştur
//    private void createKeycloakUser(String token, String email, String password, String fullName) {
//        String[] parts = fullName.split(" ", 2);
//        String firstName = parts[0];
//        String lastName = parts.length > 1 ? parts[1] : "";
//
//        Map<String, Object> user = Map.of(
//                "email", email,
//                "firstName", firstName,
//                "lastName", lastName,
//                "enabled", true,
//                "emailVerified", true,
//                "credentials", List.of(Map.of(
//                        "type", "password",
//                        "value", password,
//                        "temporary", false
//                )));
//
//        try {
//            RestClient.create(keycloakUrl).post()
//                    .uri("/admin/realms/{realm}/users", realm)
//                    .header("Authorization", "Bearer " + token)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .body(user)
//                    .retrieve()
//                    .toBodilessEntity();
//        } catch (HttpClientErrorException e) {
//            if (e.getStatusCode() == HttpStatus.CONFLICT) {
//                throw new RegistrationFailedException("Bu email zaten kayıtlı: " + email, e);
//            }
//            throw new RegistrationFailedException("Keycloak user oluşturma hatası: " + e.getMessage(), e);
//        }
//    }
//
//    // Email ile Keycloak user ID bul
//    @SuppressWarnings("unchecked")
//    private String findUserIdByEmail(String token, String email) {
//        List<Map<String, Object>> users = RestClient.create(keycloakUrl).get()
//                .uri("/admin/realms/{realm}/users?email={email}&exact=true", realm, email)
//                .header("Authorization", "Bearer " + token)
//                .retrieve()
//                .body(List.class);
//
//        return (users != null && !users.isEmpty())
//                ? users.get(0).get("id").toString()
//                : null;
//    }
//}