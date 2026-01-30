package com.banking_microservices.user_service.service;


import com.banking_microservices.user_service.client.MoneyServiceClient;
import com.banking_microservices.user_service.dto.KafkaTransactionTopicMessageDto;
import com.banking_microservices.user_service.dto.UsersDto;
import com.banking_microservices.user_service.exception.*;
import com.banking_microservices.user_service.kafka.KafkaSender;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.gson.Gson;

import java.util.List;

@Service
@Slf4j
public class UserService {

    private Gson gson = new Gson();
    @Autowired
    private UserRepository UserRepository;
    private MoneyServiceClient moneyServiceClient;
    private final KafkaSender kafkaSender;

    public UserService(UserRepository UserRepository, MoneyServiceClient moneyServiceClient, KafkaSender kafkaSender) {
        this.UserRepository = UserRepository;
        this.moneyServiceClient = moneyServiceClient;
        this.kafkaSender = kafkaSender;
    }

    @Transactional
    public List<Users> getAllUsers(){
        return UserRepository.findAll();
    }

    @Transactional
    public Users findUserByMail(String mail) {
        log.info("findUserByMail sorgusu icin parametre {}", gson.toJson(mail));
        return UserRepository.findUsersByMail(mail)
                .orElseThrow(() -> new MailNotFoundException("Mail Not Found: {}" + mail));
    }

    public UsersDto saveUser(UsersDto usersDto) {

        if (UserRepository.existsBymail(usersDto.getMail())) {
            log.info("Mail already exists In Service.saveUser and Values =  {}", gson.toJson(usersDto));
            throw new UserAlreadyExistsException("Mail Already Exists " + usersDto.getMail());
        }

        Users newUsers = Users
                .builder()
                .mail(usersDto.getMail())
                .password(usersDto.getPassword())
                .name(usersDto.getName())
                .surname(usersDto.getSurname())
                .role("USER")
                .build();

        try {
            Users user = UserRepository.save(newUsers);
            log.info("User Olusturuldu! {}", gson.toJson(user));
            try{

                kafkaSender.sendCreateUser(user.getId());
                return usersDto;

            }catch (Exception e){

                throw new KafkaSendException("Kafka ile Create user topicine mesaj gonderilirken hata olustu.");

            }

        } catch (Exception e) {
            log.warn("UserRepository.save(newUsers); Sorgusunda Hata olustu.  {} --- {}", gson.toJson(newUsers), e.getMessage());
            throw new UserSaveDatabaseException("User veritabanina kaydedilirken bir sorun olustu " + e.getMessage() + gson.toJson(newUsers));
        }
    }

    //
    // Transaction Topic Message
    //

    public void transactionTopicMessageVerify(KafkaTransactionTopicMessageDto dto){

    }

    public void UsernameValidation(KafkaTransactionTopicMessageDto dto){
        try{
            UserRepository.existsByNameAndSurname(dto.getName(), dto.getSurname());
            kafkaSender.sendUsernameValidationSuccess(dto.getEventUUID(),dto);
        }catch (Exception e){
            kafkaSender.sendUsernameValidationError(dto.getEventUUID(), dto);
            throw new UserNameOrSurnameNotFoundException("User Name Or Surname Not Found "+ dto.getName() + " " + dto.getSurname() );
        }
    }





}


