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



    public Users findUsersById(String id){
        return repository.getUsersById(id)
                .orElseThrow(() -> new UserNotFoundByIdException("User Not Found By Id. Details ==== " + id));
    }


}
