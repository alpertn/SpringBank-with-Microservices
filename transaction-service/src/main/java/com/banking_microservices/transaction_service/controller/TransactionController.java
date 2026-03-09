package com.banking_microservices.transaction_service.controller;

import com.banking_microservices.transaction_service.dto.Transaction;
import com.banking_microservices.transaction_service.dto.TransactionHistory;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.service.TransactionService;
import com.google.gson.Gson;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {
    private final Gson gson = new Gson();
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> transactionEntity(@Valid @RequestBody Transaction data) {

        transactionService.createTransaction(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("Status", 1));
    }

    @PostMapping("/gettransactionhistorywithid")
    public ResponseEntity<List<TransactionEntity>> getTransactionHistoryWithId(@Valid @RequestBody String id){
        List<TransactionEntity> transactionList = transactionService.getTransactionHistory(id);
        return ResponseEntity.ok(transactionList);
    }
}
