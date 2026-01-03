package com.springbank.user_service.controller;

import com.springbank.user_service.dto.CheckUserBalanceDto;
import com.springbank.user_service.model.Users;
import com.springbank.user_service.service.service;
import jakarta.validation.constraints.NotEmpty;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class Controller {

    private service service;

    public Controller(service service) {
        this.service = service;
    }

    @GetMapping("/getallusers")
    public ResponseEntity<List<Users>> getAllUsers(){
        return ResponseEntity.ok(service.getAllUsers());
    }

    @PostMapping("/finduserbyiban")
    public ResponseEntity<Users> findUserByIban(@NotEmpty String iban){
        return ResponseEntity.ok(service.findUserByIban(iban));
    }

    @PostMapping("/checkusersbalancewithiban")
    public ResponseEntity<CheckUserBalanceDto> checkUser(@NotEmpty String senderIban, String receiverIban){
        return ResponseEntity.ok(service.checkUsersBalance(senderIban,receiverIban));
    }

    @PostMapping("/finduserbyid")
    public ResponseEntity<Users> findUserById(@NotEmpty String uuid){
        return ResponseEntity.ok(service.findUsersById(uuid));
    }

    @PostMapping("/deleteusermoneywithiban")
    public ResponseEntity<Boolean> deleteUserMoneyWithIban(@NotEmpty String iban, @NotEmpty String balance){

        if (service.DeleteMoneyByIban(iban,Double.valueOf(balance)) == true){
            return ResponseEntity.ok().build();
        }
        return (ResponseEntity<Boolean>) ResponseEntity.notFound();

    }

}
