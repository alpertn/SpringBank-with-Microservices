package com.springbank.money_service.controller;

import com.springbank.money_service.exception.IbanNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(IbanNotFoundException.class)
//    public ResponseEntity<Map<String, Object>> handleIbanNotFound(IbanNotFoundException ex){
//
//        Map<String,Object> map = new HashMap<>();
//
//        map.put("timestamp" , LocalDateTime.now());
//        map.put("status", HttpStatus.NOT_FOUND.value());
//        map.put("error", "Iban Bulunamadi");
//        map.put("errorCode", ""); // BURAYA SECURITY VAULT CONFIG EKLENECEK
//
//
//    }


}
