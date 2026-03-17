package com.banking_microservices.transaction_service.controller;

import com.banking_microservices.transaction_service.dto.Transaction;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.service.TransactionService;
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

// DUZELTME: Gson import kaldirildi - controller icinde hic kullanilmiyordu (dead import/field).

@RestController
@RequestMapping("/api/transaction-service/v1/transactions")
@Slf4j
public class TransactionController {

    // DUZELTME: private final Gson gson = new Gson() satiri kaldirildi.
    // Hic kullanilmiyordu, gereksiz bagimlilik olusturuyordu.

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
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

        log.info("Transaction Service TransactionController transactionEntity Modulu Istegi aldi.  id : {}", userId);

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

        log.info("Transaction Service TransactionController deposit Modulu Istegi aldi.  id : {}", userId);

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

        log.info("Transaction Service TransactionController withdraw Modulu Istegi aldi.  id : {}", userId);

        transactionService.createWithdraw(data, userId, userEmail, userName, userSurname);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("Status", 1));
    }

    // DUZELTME: onceden endpoint @PostMapping ile tanimlanmisti ve body'den String aliyordu.
    // ID almak icin POST + RequestBody String kullanmak yanlis - GET + @PathVariable ya da
    // @RequestParam olmali. GET ile degistirildi ve @RequestParam kullanildi.
    // Ayrica metodun adi getTransactionHistoryWithId olarak daha aciklayici hale getirildi.
    @GetMapping("/gettransactionhistorywithid")
    public ResponseEntity<List<TransactionEntity>> getTransactionHistoryWithId(
            @RequestParam String id) {
        List<TransactionEntity> transactionList = transactionService.getTransactionHistory(id);
        return ResponseEntity.ok(transactionList);
    }

    // DUZELTME: TransactionService'de getTransactionsByDateRange ve getErrorLogs ve getTransactionById
    // metodlari vardi fakat controller'da hic endpoint tanimlanmamisti. Eksik endpointler eklendi.
    @GetMapping("/errors")
    public ResponseEntity<List<TransactionEntity>> getErrorLogs() {
        log.info("Transaction Service TransactionController getErrorLogs Modulu Istegi aldi.");
        List<TransactionEntity> errorList = transactionService.getErrorLogs();
        return ResponseEntity.ok(errorList);
    }

    @GetMapping("/daterange")
    public ResponseEntity<List<TransactionEntity>> getTransactionsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        log.info("Transaction Service TransactionController getTransactionsByDateRange Modulu Istegi aldi.");
        List<TransactionEntity> transactionList = transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(transactionList);
    }

    @GetMapping("/byid")
    public ResponseEntity<TransactionEntity> getTransactionById(@RequestParam String id) {
        log.info("Transaction Service TransactionController getTransactionById Modulu Istegi aldi. id : {}", id);
        TransactionEntity transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }
}