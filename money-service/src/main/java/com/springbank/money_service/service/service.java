package com.springbank.money_service.service;

import com.springbank.money_service.dto.moneyDto;
import com.springbank.money_service.exception.IbanNotFoundException;
import com.springbank.money_service.repository.repository;
import org.springframework.stereotype.Service;

import java.awt.print.Book;
import java.util.Optional;

@Service
public class service {

    private repository repository;

    public Double getUserMoneyByIban(String iban){
        return repository.findMoneyByIban(iban)
                .map(money -> money.getBalance())
                .orElseThrow(() -> new IbanNotFoundException("Iban Not Found " + iban));
    }

    public Boolean setUserMoneyByIban(String iban, Double balance){
        if (repository.updateMoneyByIban(iban,balance) != 0){
            return true;
        }else{
            return false;
        }
    }

}
