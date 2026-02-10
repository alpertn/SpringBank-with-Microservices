package com.banking_microservices.user_service.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersDto {

    private String mail;
    private String password;
    @Builder.Default
    private String name = "Ahmet";
    @Builder.Default
    private String surname = "Kar";
}
