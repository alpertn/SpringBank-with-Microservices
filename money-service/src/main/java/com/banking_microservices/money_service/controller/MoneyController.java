package com.banking_microservices.money_service.controller;

import com.banking_microservices.money_service.dto.TransactionRequestDto;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.banking_microservices.money_service.service.UserMoneyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;


@RestController
@RequestMapping("/api/money-service/v1/accounts")
@Slf4j
public class MoneyController {

    // Localdatetime parse hatasini onlemek + serıalızenulls ıcın gson serıalızer
    // olusturuldu.
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type,
                            ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type,
                            ctx) -> java.time.LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();
    private final UserMoneyRepository userMoneyRepository;
    private final UserMoneyService userMoneyService;
    private final Supplier<String> currentTime;

    public MoneyController(UserMoneyRepository userMoneyRepository, UserMoneyService userMoneyService,
            Supplier<String> currentTime) {
        this.userMoneyRepository = userMoneyRepository;
        this.userMoneyService = userMoneyService;
        this.currentTime = currentTime;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UserMoney UserMoneyService is healthy");
    }

    @PostMapping("/createusermoney")
    public ResponseEntity<?> userOlustur(@RequestHeader("X-User-KeycloakUUID") String keycloakUserUUID) {
        log.info(" ({}) > MoneyController | userOlustur -> User Id Parametresi Headerdan geldi.\n{}", currentTime.get(), gson.toJson(keycloakUserUUID));
        return ResponseEntity.ok(userMoneyService.generateUser(keycloakUserUUID));
    }

    @PostMapping("/getUserIbanWithUserId")
    public ResponseEntity<?> getUserIbanWithUserId(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        log.info(" ({}) > MoneyController | getUserIbanWithUserId -> getUserIbanWithUserId istegi geldi. UserId:\n{}", currentTime.get(), gson.toJson(userId));
        return ResponseEntity.ok(userMoneyService.getAccountByUserId(userId));
    }

    @GetMapping("/balance-info")
    public ResponseEntity<?> getBalanceAndIban(@RequestHeader("X-User-KeycloakUUID") String userId) {
        log.info(" ({}) > MoneyController | getBalanceAndIban -> Money Service MoneyController getBalanceAndIban Modulu Istegi aldi. id :\n{}", currentTime.get(), gson.toJson(userId));
        return ResponseEntity.ok(userMoneyService.getAccountByUserId(userId));
    }

    @PostMapping("/depositByUserId")
    public ResponseEntity<?> depositByUserId(@RequestBody TransactionRequestDto body) {
        String userId = body.getUserId();
        java.math.BigDecimal amount = body.getAmount();
        log.info(" ({}) > MoneyController | depositByUserId -> Deposit istegi. UserId:\n{}, Miktar:\n{}", currentTime.get(), gson.toJson(userId), gson.toJson(amount));
        userMoneyService.depositMoneyByUserId(userId, amount);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Para yatırma başarılı"));
    }

    @PostMapping("/withdrawByUserId")
    public ResponseEntity<?> withdrawByUserId(@RequestBody TransactionRequestDto body) {
        String userId = body.getUserId();
        java.math.BigDecimal amount = body.getAmount();
        log.info(" ({}) > MoneyController | withdrawByUserId -> Withdraw istegi. UserId:\n{}, Miktar:\n{}", currentTime.get(), gson.toJson(userId), gson.toJson(amount));
        userMoneyService.withdrawMoneyByUserId(userId, amount);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Para çekme başarılı"));
    }
}
