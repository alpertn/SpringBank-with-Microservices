package com.banking_microservices.money_service.controller;

import com.banking_microservices.money_service.dto.IdDto;
import com.banking_microservices.money_service.dto.TransactionRequestDto;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.banking_microservices.money_service.service.UserMoneyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Money-Service Controller Classı
 * 
 * Aldigi veriler ile {@link UserMoneyService} ve {@link UserMoneyRepository}
 * classlarini cagirir.
 * 
 * 
 * Endpointler
 * 1 - /api/money-service/v1/accounts/createusermoney
 * 2 - /api/money-service/v1/accounts/getUserIbanWithUserId
 * 3 - /api/money-service/v1/accounts/balance-info
 * 4 - /api/money-service/v1/accounts/depositByUserId
 * 5 - /api/money-service/v1/accounts/withdrawByUserId
 * 
 */
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

    /**
     * 
     * Bu method userId kullanarak user money hesabı olusturur.
     * {@link UserMoneyService} createUser isteği icin service'ye gider. service de keycloak a istek atar.
     *
     * @param userId RequestBody'den gelen userId {@link IdDto} verisidir.
     * @return Request Response olarak olusturdugu user money modelını dondurur.
     *         iban userid ve money degerlerini icerir.
     */
    @PostMapping("/createusermoney")
    public ResponseEntity<?> userOlustur(@Valid @RequestBody IdDto userId) {
        log.info(" ({}) > MoneyController | userOlustur -> User Id Parametresi geldi. {}", currentTime.get(), gson.toJson(userId));
        return ResponseEntity.ok(userMoneyService.generateUser(userId.getId()));
    }

    /**
     * 
     * id ile IBAN sorgu. {@link UserMoneyService} ye istek gonderir.
     * 
     * @param body jsondan gelen userId s
     * @return MoneyDto kullanıcının tum bılgılerı.
     */
    @PostMapping("/getUserIbanWithUserId")
    public ResponseEntity<?> getUserIbanWithUserId(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        log.info(" ({}) > MoneyController | getUserIbanWithUserId -> getUserIbanWithUserId istegi geldi. UserId: {}", currentTime.get(), gson.toJson(userId));
        return ResponseEntity.ok(userMoneyService.getAccountByUserId(userId));
    }

    /**
     *
     * {@link UserMoneyService} class ina istek gonderir aldigi id ile ve kullanicinin bilgilerini dondurur.
     *
     * @param userId @RequestHeader("X-User-KeyloackId") Headerdeki veriyi alir.
     * @return
     */
    @GetMapping("/balance-info")
    public ResponseEntity<?> getBalanceAndIban(@RequestHeader("X-User-KeyloackId") String userId) {
        log.info(" ({}) > MoneyController | getBalanceAndIban -> Money Service MoneyController getBalanceAndIban Modulu Istegi aldi. id : {}", currentTime.get(), gson.toJson(userId));
        return ResponseEntity.ok(userMoneyService.getAccountByUserId(userId));
    }

    /**
     * 
     * Kullanici idsi ile deposit islemi. hesaba para ekleme.
     *
     * Aklıma gelen guvenlık acıgı : adam kendi idsini deiglde istedigi idyi gonderebilir jwtden gelen headerdeki idyi kullansak daha iyi
     * Admin panel uses this endpoint to deposit into user accounts, so we strictly type the body as TransactionRequestDto.
     *
     * @param body request bodyden gelen userId ve amount degeri.
     * @return Response 200
     */
    @PostMapping("/depositByUserId")
    public ResponseEntity<?> depositByUserId(@RequestBody TransactionRequestDto body) {
        String userId = body.getUserId();
        java.math.BigDecimal amount = body.getAmount();
        log.info(" ({}) > MoneyController | depositByUserId -> Deposit istegi. UserId: {}, Miktar: {}", currentTime.get(), gson.toJson(userId), gson.toJson(amount));
        userMoneyService.depositMoneyByUserId(userId, amount);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Para yatırma başarılı"));
    }

    /**
     * 
     * Kullanici id ile withdraw.
     *
     * @param body request bodyden gelen userId ve amount degeri
     * @return response 200 veya exception donuyo
     */
    @PostMapping("/withdrawByUserId")
    public ResponseEntity<?> withdrawByUserId(@RequestBody TransactionRequestDto body) {
        String userId = body.getUserId();
        java.math.BigDecimal amount = body.getAmount();
        log.info(" ({}) > MoneyController | withdrawByUserId -> Withdraw istegi. UserId: {}, Miktar: {}", currentTime.get(), gson.toJson(userId), gson.toJson(amount));
        userMoneyService.withdrawMoneyByUserId(userId, amount);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Para çekme başarılı"));
    }
}
