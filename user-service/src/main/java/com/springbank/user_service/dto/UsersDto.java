package com.springbank.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersDto {


    private String id;
    private String tckimlikNo;
    private String iban;
    private String password;
    private Double customerBalance;
    private String customerName;
    private String customerSurname;
}
