package com.banking_microservices.auth_service.service;

import com.banking_microservices.auth_service.dto.RegisterDto;
import com.banking_microservices.auth_service.exception.KeycloackUserCreateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    private KeycloackAdminService keycloackAdminService;
    private KeycloackUserService keycloackUserService;


    public void createUser(RegisterDto dto){

        try{
            String keycloackUserUUID = keycloackAdminService.createKeycloackUser(dto);
        }catch (Exception e){
            throw new KeycloackUserCreateException("An exception with Keycloack create user");
        }

        try{

        }

    }



}
