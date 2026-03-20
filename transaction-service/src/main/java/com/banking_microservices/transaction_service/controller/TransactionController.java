package com.banking_microservices.transaction_service.controller;

import com.banking_microservices.transaction_service.dto.Transaction;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.service.TransactionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction-service/v1/transactions")
@Slf4j
public class TransactionController {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();
    private final TransactionService transactionService;
    private final java.util.function.Supplier<String> currentTime;

    public TransactionController(TransactionService transactionService, java.util.function.Supplier<String> currentTime) {
        this.transactionService = transactionService;
        this.currentTime = currentTime;
    }

    @PostMapping("/create")
    public ResponseEntity<?> transactionEntity(
            @RequestHeader(value = "X-User-KeyloackId", required = false) String userId,
            @RequestHeader(value = "X-User-Username", required = false) String userUsername,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Surname", required = false) String userSurname,

            @Valid @RequestBody Transaction data) {

        log.info(" ({}) > TransactionController | transactionEntity -> Istek alindi. UserId : {}, Dto: {}", currentTime.get(), userId, gson.toJson(data));

        transactionService.createTransaction(data, userId, userEmail, userName, userSurname);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("Status", 1));
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(
            @RequestHeader(value = "X-User-KeyloackId", required = false) String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Surname", required = false) String userSurname,
            @Valid @RequestBody com.banking_microservices.transaction_service.dto.DepositDto data) {

        log.info(" ({}) > TransactionController | deposit -> Istek alindi. UserId : {}, Dto: {}", currentTime.get(), userId, gson.toJson(data));

        transactionService.createDeposit(data, userId, userEmail, userName, userSurname);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("Status", 1));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @RequestHeader(value = "X-User-KeyloackId", required = false) String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Surname", required = false) String userSurname,
            @Valid @RequestBody com.banking_microservices.transaction_service.dto.WithdrawDto data) {

        log.info(" ({}) > TransactionController | withdraw -> Istek alindi. UserId : {}, Dto: {}", currentTime.get(), userId, gson.toJson(data));

        transactionService.createWithdraw(data, userId, userEmail, userName, userSurname);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("Status", 1));
    }

    @GetMapping("/gettransactionhistorywithid")
    public ResponseEntity<List<TransactionEntity>> getTransactionHistoryWithId(
            @RequestParam String id) {
        log.info(" ({}) > TransactionController | getTransactionHistoryWithId -> Istek alindi. Id : {}", currentTime.get(), id);
        List<TransactionEntity> transactionList = transactionService.getTransactionHistory(id);
        return ResponseEntity.ok(transactionList);
    }

    @GetMapping("/errors")
    public ResponseEntity<List<TransactionEntity>> getErrorLogs() {
        log.info(" ({}) > TransactionController | getErrorLogs -> Istek alindi.", currentTime.get());
        List<TransactionEntity> errorList = transactionService.getErrorLogs();
        return ResponseEntity.ok(errorList);
    }

    @GetMapping("/daterange")
    public ResponseEntity<List<TransactionEntity>> getTransactionsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        log.info(" ({}) > TransactionController | getTransactionsByDateRange -> Istek alindi. StartDate : {}, EndDate : {}", currentTime.get(), startDate, endDate);
        List<TransactionEntity> transactionList = transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(transactionList);
    }

    @GetMapping("/byid")
    public ResponseEntity<TransactionEntity> getTransactionById(@RequestParam String id) {
        log.info(" ({}) > TransactionController | getTransactionById -> Istek alindi. Id : {}", currentTime.get(), id);
        TransactionEntity transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }
}