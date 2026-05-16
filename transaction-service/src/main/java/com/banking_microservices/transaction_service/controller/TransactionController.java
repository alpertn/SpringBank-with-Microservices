package com.banking_microservices.transaction_service.controller;

import com.banking_microservices.transaction_service.dto.TransactionRequestDto;
import com.banking_microservices.transaction_service.dto.TokenDetailsDto;
import com.banking_microservices.transaction_service.grpc.AuthTokenGrpcClient;
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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
            .setPrettyPrinting()
            .create();
    private final TransactionService transactionService;
    private final Supplier<String> currentTime;
    private final AuthTokenGrpcClient authTokenGrpcClient;

    public TransactionController(TransactionService transactionService, Supplier<String> currentTime,
                                 AuthTokenGrpcClient authTokenGrpcClient) {
        this.transactionService = transactionService;
        this.currentTime = currentTime;
        this.authTokenGrpcClient = authTokenGrpcClient;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTransaction(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-User-KeycloakUUID", required = false) String keycloakUUID,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Surname", required = false) String userSurname,
            @Valid @RequestBody TransactionRequestDto data) {

        TokenDetailsDto tokenDetails = authTokenGrpcClient.decodeToken(authorizationHeader);
        data.setTokenDetails(tokenDetails);

        log.info(" ({}) > TransactionController | createTransaction -> Istek alindi. KeycloakUUID: {}, Dto:\n{}", currentTime.get(), keycloakUUID, gson.toJson(data));

        transactionService.createTransaction(data, keycloakUUID, userEmail, userName, userSurname, tokenDetails);
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

    @PostMapping("/cancel")
    public ResponseEntity<TransactionEntity> cancelTransaction(
            @RequestHeader(value = "X-User-KeycloakUUID", required = false) String keycloakUUID,
            @RequestParam String eventUUID) {
        log.info(" ({}) > TransactionController | cancelTransaction -> Istek alindi. EventUUID: {}, User: {}",
                currentTime.get(), eventUUID, keycloakUUID);
        TransactionEntity transaction = transactionService.cancelTransaction(eventUUID, keycloakUUID, false);
        return ResponseEntity.ok(transaction);
    }
}
