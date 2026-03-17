package com.banking_microservices.money_service.controller;

import com.banking_microservices.money_service.dto.IdDto;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.banking_microservices.money_service.service.UserMoneyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;

// DUZELTME: @RequiredArgsConstructor kaldirildi. Lombok bu anotasyon ile tum final field'lari
// constructor'a inject etmeye calisir. Gson bir Spring bean olmadigi icin
// @RequiredArgsConstructor + final Gson birlikte kullanildiginda uygulama ayaga kalkarken
// BeanCreationException ile fail edip 500 donuyordu. Manuel constructor yazildi.
@RestController
@RequestMapping("/api/money-service/v1/accounts")
@Slf4j
public class MoneyController {

    // DUZELTME: Gson Spring tarafindan inject edilmemeli, new ile olusturuldu.
    private final Gson gson = new Gson();
    private final UserMoneyRepository userMoneyRepository;
    private final UserMoneyService userMoneyService;

    public MoneyController(UserMoneyRepository userMoneyRepository, UserMoneyService userMoneyService) {
        this.userMoneyRepository = userMoneyRepository;
        this.userMoneyService = userMoneyService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UserMoney UserMoneyService is healthy");
    }

    @PostMapping("/createusermoney")
    public ResponseEntity<?> userOlustur(@Valid @RequestBody IdDto userId) {
        String gsonLog = gson.toJson(userId);
        log.info("User Id Parametresi geldi. {}", gsonLog);
        return ResponseEntity.ok(userMoneyService.generateUser(userId.getId()));
    }

    // Kullanici ID ile IBAN (Hesap) sorgulama
    @PostMapping("/getUserIbanWithUserId")
    public ResponseEntity<?> getUserIbanWithUserId(@RequestBody java.util.Map<String, String> body) {
        String userId = body.get("userId");
        log.info("getUserIbanWithUserId istegi geldi. UserId: {}", userId);
        return ResponseEntity.ok(userMoneyService.getAccountByUserId(userId));
    }

    @GetMapping("/balance-info")
    public ResponseEntity<?> getBalanceAndIban(@RequestHeader("X-User-KeyloackId") String userId) {
        log.info("Money Service MoneyController getBalanceAndIban Modulu Istegi aldi.  id : {}", userId);
        return ResponseEntity.ok(userMoneyService.getAccountByUserId(userId));
    }

    // Kullanici ID ile Para Yatirma (Kendi Hesabina)
    @PostMapping("/depositByUserId")
    public ResponseEntity<?> depositByUserId(@RequestBody java.util.Map<String, String> body) {
        String userId = body.get("userId");
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount"));
        log.info("Deposit istegi. UserId: {}, Miktar: {}", userId, amount);
        userMoneyService.depositMoneyByUserId(userId, amount);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Para yatırma başarılı"));
    }

    // Kullanici ID ile Para Cekme (Kendi Hesabindan)
    @PostMapping("/withdrawByUserId")
    public ResponseEntity<?> withdrawByUserId(@RequestBody java.util.Map<String, String> body) {
        String userId = body.get("userId");
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount"));
        log.info("Withdraw istegi. UserId: {}, Miktar: {}", userId, amount);
        userMoneyService.withdrawMoneyByUserId(userId, amount);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Para çekme başarılı"));
    }
}