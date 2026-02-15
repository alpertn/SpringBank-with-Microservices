package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.RegisterDto;
import com.banking_microservices.auth_service.exception.KeycloackUserCreateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

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



    public String createKeycloackUser(RegisterDto dto){
        Map<String,Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", dto.getPassword());
        credentials.put("temporary", false);

        Map<String,Object> userMap = new HashMap<>();

        userMap.put("email", dto.getEmail());
        userMap.put("name", dto.getName());
        userMap.put("surname", dto.getSurname());
        userMap.put("enabled", true);
        userMap.put("credentials", List.of(credentials));

        try{
            var keycloackResponse = RestClient.create(keycloakUrl).post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header("Authorization", "Bearer " + getAdminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userMap)
                    .retrieve()
                    .toBodilessEntity();

            String location = keycloackResponse.getHeaders().getFirst(HttpHeaders.LOCATION);

            if(location == null){
                throw new KeycloackUserCreateException("Keycloack Headerinden veri alinamadi");
            }

            String userId = location.substring(location.lastIndexOf('/') + 1); // FORMATLAMA
            return userId;
        }catch (Exception e){
            throw new KeycloackUserCreateException("An Exception in user create on keycloack.");
        }

    }




    public Boolean existsByEmail(String email, String adminToken) {

        // Intellij idea ve Javadaki hatalar yuzunden hata olmamasina ragmen hata veriyor. o yuzden bunu kullandim has kod altta.
        RestClient customClient = RestClient.builder()
                .baseUrl(keycloakUrl)
                .defaultHeader("Authorization", "Bearer " + adminToken) // Token'ı buraya verdik!
                .build();

        List<Map<String, Object>> users = customClient.get()
                .uri("/admin/realms/{realm}/users?email={email}&exact=true", realm, email)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});


//        List<Map<String, Object>> users = RestClient.create(keycloakUrl).get()
//                .uri("/admin/realms/{realm}/users?email={email}&exact=true", realm, email)
//                .header("Authorization", "Bearer " + adminToken)
//                .retrieve()
//                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {}); // Duz list donduremiyoruz, build patternde Raw type List olunca compiler hata veriyor.


        if (users == null || users.isEmpty()) {
            return false;
        } else {
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
