package com.springbank.user_service.service;


import com.springbank.user_service.exception.UserNotFoundException;
import com.springbank.user_service.model.Users;
import com.springbank.user_service.dto.UsersBalanceDto;
import com.springbank.user_service.repository.repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class service {

    private final repository repository;

    public service(repository repository) {
        this.repository = repository;
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

    public List<UsersBalanceDto> checkUsersBalance(String iban1, String iban2){
        return
    }



}
