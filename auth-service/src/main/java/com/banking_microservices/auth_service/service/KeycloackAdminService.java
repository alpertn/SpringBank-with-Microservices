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

    import java.util.Collections;
    import java.util.HashMap;
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

        private Keycloak keycloak;

        // keycloack config
        @PostConstruct
        public void init(){
            this.keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakUrl)
                    .realm("master")
                    .username(adminUsername)
                    .password(adminPassword)
                    .clientId("admin-cli")
                    .build();
        }

        public String createKeycloakUser(RegisterDto register, Role role){

            if (existsByEmail(register.getEmail())){
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

            // jakartanin response kullaniyoruz keycloaka ait degil realmina istek gonderip responseyi aliyor
            Response response = keycloak.realm(realm).users().create(user);
            // Responseden User Id cekme.
            String userId = response.getHeaderString("Location").substring(response.getHeaderString("Location").lastIndexOf('/') + 1);
            // http istegi acik kaldigi icin kapatmamiz lazim.
            response.close();

            assignRole(userId,role.name());

            return userId;



        }

        private void assignRole(String userId, String roleName) {
            try {
                //  try catch blogu ile http istegi otomatik kapandigi icin kapatmamzia gerek kalmiyor
                var role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
                keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
            } catch (Exception e) {
                throw new KeycloackUserCreateException("An exception with assign role in keycloak");
            }
        }

        public boolean existsByEmail(String email) {
            List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
            return users != null && !users.isEmpty();
        }

//        public boolean existsById(String userId) {
//            try {
//                UserRepresentation user = keycloak.realm(realm).users()
//                        .get(userId)
//                        .toRepresentation();
//                return user != null;
//            } catch (Exception e) {
//                return false;
//            }
//        }

//        public UserRepresentation getUserById(String userId) {
//            try {
//                return keycloak.realm(realm).users()
//                        .get(userId)
//                        .toRepresentation();
//            } catch (Exception e) {
//                log.error("User not found with id: {}", userId);
//                return null;
//            }
//        }
//
//        public UserRepresentation getUserByEmail(String email) {
//            List<UserRepresentation> users = keycloak.realm(realm).users()
//                    .searchByEmail(email, true);
//
//            if (users != null && !users.isEmpty()) {
//                return users.get(0);
//            }
//            return null;
//        }
//
//        public void deleteUser(String userId) {
//            try {
//                keycloak.realm(realm).users().delete(userId);
//                log.info("User deleted: {}", userId);
//            } catch (Exception e) {
//                log.error("User deletion failed: {}", userId, e);
//                throw new KeycloackUserCreateException("User deletion failed");
//            }
//        }

    }


//public String createKeycloackUser(RegisterDto dto){
//            Map<String,Object> credentials = new HashMap<>();
//            credentials.put("type", "password");
//            credentials.put("value", dto.getPassword());
//            credentials.put("temporary", false);
//
//            Map<String,Object> userMap = new HashMap<>();
//
//            userMap.put("email", dto.getEmail());
//            userMap.put("name", dto.getName());
//            userMap.put("surname", dto.getSurname());
//            userMap.put("enabled", true);
//            userMap.put("credentials", List.of(credentials));
//
//            try{
//                var keycloackResponse = RestClient.create(keycloakUrl).post()
//                        .uri("/admin/realms/{realm}/users", realm)
//                        .header("Authorization", "Bearer " + getAdminToken())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .body(userMap)
//                        .retrieve()
//                        .toBodilessEntity();
//
//                String location = keycloackResponse.getHeaders().getFirst(HttpHeaders.LOCATION);
//
//                if(location == null){
//                    throw new KeycloackUserCreateException("Keycloack Headerinden veri alinamadi");
//                }
//
//                String userId = location.substring(location.lastIndexOf('/') + 1); // FORMATLAMA
//                return userId;
//            }catch (Exception e){
//                throw new KeycloackUserCreateException("An Exception in user create on keycloack.");
//            }
//
//        }
//
//
//
//
//        public Boolean existsByEmail(String email, String adminToken) {
//
//            // Intellij idea ve Javadaki hatalar yuzunden hata olmamasina ragmen hata veriyor. o yuzden bunu kullandim has kod altta.
//            RestClient customClient = RestClient.builder()
//                    .baseUrl(keycloakUrl)
//                    .defaultHeader("Authorization", "Bearer " + adminToken) // Token'ı buraya verdik!
//                    .build();
//
//            List<Map<String, Object>> users = customClient.get()
//                    .uri("/admin/realms/{realm}/users?email={email}&exact=true", realm, email)
//                    .retrieve()
//                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
//
//
//    //        List<Map<String, Object>> users = RestClient.create(keycloakUrl).get()
//    //                .uri("/admin/realms/{realm}/users?email={email}&exact=true", realm, email)
//    //                .header("Authorization", "Bearer " + adminToken)
//    //                .retrieve()
//    //                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {}); // Duz list donduremiyoruz, build patternde Raw type List olunca compiler hata veriyor.
//
//
//            if (users == null || users.isEmpty()) {
//                return false;
//            } else {
//                return true;
//            }
//        }
//
//        private String getAdminToken(){
//            MultiValueMap<String,String> requestParams = new LinkedMultiValueMap<>();
//            requestParams.add("grant_type", "password");
//            requestParams.add("client_id", "admin-cli");
//            requestParams.add("username", adminUsername);
//            requestParams.add("password", adminPassword);
//
//            Map<String,Object> token = RestClient.create(keycloakUrl).post()
//                    .uri("/realms/master/protocol/openid-connect/token")
//                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                    .body(requestParams)
//                    .retrieve()
//                    .body(Map.class);
//
//            if(token == null  ||  !token.containsKey("access_token")){
//                return null;
//            }else{
//                return token.get("access_token").toString();
//            }
//        }