package com.springbank.user_service.service;


import com.springbank.user_service.dto.CheckUserBalanceDto;
import com.springbank.user_service.exception.BalanceException;
import com.springbank.user_service.exception.UserNotFoundByIdException;
import com.springbank.user_service.exception.UserNotFoundException;
import com.springbank.user_service.model.Users;
import com.springbank.user_service.dto.UsersBalanceDto;
import com.springbank.user_service.repository.repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class service {

    private final repository repository;

    public service(repository repository) {
        this.repository = repository;
    }


    public List<Users> getAllUsers(){
        return repository.findAll()
                .stream()
                .collect(Collectors.toList());
    }

    public Users findUserByIban(String iban){

        return repository.getUsersByIban(iban)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By Iban. Details === " + iban));

    }

    public Users findUserByTCKN(String tckn){
        return repository.getUsersByTckimlikNo(tckn)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By TCKN. Details === " + tckn));
    }

    public UsersBalanceDto findUsesrBalanceWithIban(String iban){
        return repository.getUsersByIban(iban)
                .map(Users -> new UsersBalanceDto(Users.getId(),Users.getIban(),Users.getCustomerBalance()))
                .orElseThrow(() -> new UserNotFoundException("User Balance Not Found With Iban" + iban));
    }

    public CheckUserBalanceDto checkUsersBalance(String senderIban, String receiverIban){ // repo zaten optional<Users> oldugu icin her turlu users aliyoruz ve users aldigimiz veriden usersin icindeki 2 - 3 veriyi cekip yeni checkuserbalance dto olusturuyoruz

        Users senderUser = repository.getUsersByIban(senderIban)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By Iban. Details ===" + senderIban));

        Users receiverUser = repository.getUsersByIban(receiverIban)
                .orElseThrow(() -> new UserNotFoundException("User Not Found By Iban. Details ===" + receiverIban));


        return CheckUserBalanceDto.builder()
                .senderUserId(senderUser.getId())
                .receiverUserId(receiverUser.getId())
                .senderIban(senderUser.getIban())
                .receiverIban(receiverUser.getIban())
                .senderUserBalance(Double.valueOf(senderUser.getCustomerBalance()))
                .receiverUserBalance(Double.valueOf(receiverUser.getCustomerBalance())).build();

    }

    public Users findUsersById(String id){
        return repository.getUsersById(id)
                .orElseThrow(() -> new UserNotFoundByIdException("User Not Found By Id. Details ==== " + id));
    }

    public Boolean DeleteMoneyByIban(String iban, Double balance){
        int changes =  repository.deleteMoneyByIban(iban,balance);

        if (changes ==0) throw new BalanceException("Database Can Not Delete User's Balance. Details ==== " + "Iban = " + iban + " Balance " + balance );

        return true;
    }

}
