package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.repository.UserMoneyRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final UserMoneyService service;
    private final UserMoneyRepository repository;

    public TransactionService(UserMoneyService service, UserMoneyRepository repository) {
        this.service = service;
        this.repository = repository;
    }


    

}
