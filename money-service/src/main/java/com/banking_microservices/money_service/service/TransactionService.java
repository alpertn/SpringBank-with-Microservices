package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.money_service.exception.EventSaveException;
import com.banking_microservices.money_service.exception.EventUUIDAlreadyExists;
import com.banking_microservices.money_service.exception.IbanNotFoundException;
import com.banking_microservices.money_service.exception.MoneyNotAvaibleException;
import com.banking_microservices.money_service.kafka.KafkaListenerService;
import com.banking_microservices.money_service.kafka.KafkaSender;
import com.banking_microservices.money_service.models.KafkaLastActivity;
import com.banking_microservices.money_service.repository.KafkaLastActivityRepository;
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
    private final KafkaLastActivityRepository kafkaLastActivityRepository;

    public TransactionService(UserMoneyService service, UserMoneyRepository repository, KafkaSender kafkaSender, KafkaListenerService kafkaListenerService, KafkaLastActivityRepository kafkaLastActivityRepository) {
        this.service = service;
        this.repository = repository;
        this.kafkaSender = kafkaSender;
        this.kafkaListenerService = kafkaListenerService;
        this.kafkaLastActivityRepository = kafkaLastActivityRepository;
    }


    public void KafkaTransactionTopicService(KafkaTransactionTopicMessageDto dto){

        if(kafkaLastActivityRepository.existsByEventUUID(dto.getEventUUID())){
            throw new EventUUIDAlreadyExists("Event UUID Already exists KafkaTransactionTopicService " + dto.getEventUUID());
        }else{
            // varsa hata yoksa save ve continue
            try{
                kafkaLastActivityRepository.save(
                        KafkaLastActivity
                                .builder()
                                .eventUUID(dto.getEventUUID())
                                .build()
                );
            }catch (Exception e){
                throw new EventSaveException("Event Save exception " + dto.getEventUUID());
            }
        }

        BigDecimal balance = repository.findBalanceByIban(dto.getSenderIban()) // ORELSEGET
                .orElseGet(() -> {
                    dto.setError(true);
                    dto.setErrorDescription("Iban Number Not Found: " + dto.getSenderIban());

                    kafkaSender.sendTransactionError(dto.getEventUUID(), dto);

                    throw new IbanNotFoundException("KafkaTransactionTopicService Hesap bulunamadi." + gson.toJson(dto));
                });

        if(balance.compareTo(dto.getMoney()) > 0){ // bigdecimal oldugu icin boyle yazmam lazim.

            kafkaSender.sendTransactionToUserService(dto.getEventUUID(),dto);

        }else{
            throw new MoneyNotAvaibleException("Money not avaible KafkaTransactionTopicService");
        }

    }





}
