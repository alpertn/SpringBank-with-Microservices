package com.springbank.user_service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckUserBalanceDto {

    private String senderUserId;
    private String receiverUserId;
    private String senderIban;
    private String receiverIban;
    private double senderUserBalance;
    private double receiverUserBalance;

}
