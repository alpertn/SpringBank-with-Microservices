package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.IbanNotFoundException;
import com.banking_microservices.money_service.exception.MoneyNotAvaibleException;
import com.banking_microservices.money_service.kafka.KafkaListenerService;
import com.banking_microservices.money_service.kafka.KafkaSender;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class TransactionService {
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final UserMoneyService service;
    private final UserMoneyRepository repository;
    private final KafkaSender kafkaSender;
    private final KafkaListenerService kafkaListenerService;

    public TransactionService(UserMoneyService service, UserMoneyRepository repository, KafkaSender kafkaSender, KafkaListenerService kafkaListenerService) {
        this.service = service;
        this.repository = repository;
        this.kafkaSender = kafkaSender;
        this.kafkaListenerService = kafkaListenerService;
    }


    public void KafkaTransactionTopicService(KafkaTransactionTopicMessageDto dto){
        BigDecimal balance = repository.findBalanceByIban(dto.getSenderIban())
                .orElseThrow( () -> new IbanNotFoundException("KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto)));

        if(balance.compareTo(dto.getMoney()) > 0){

            kafkaSender.sendTransactionToUserService(dto.getEventUUID(),dto);

        }else{
            throw new MoneyNotAvaibleException("Money not avaible KafkaTransactionTopicService");
        }

    }
    

}
