package com.banking_microservices.admin_service_command.controller;

import com.banking_microservices.admin_service_command.dto.AdminHistoryCommandRequest;
import com.banking_microservices.admin_service_command.dto.AdminHistoryCommandResponse;
import com.banking_microservices.admin_service_command.service.AdminHistoryCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin-service-command/v1/history")
@RequiredArgsConstructor
public class AdminCommandController {

    private final AdminHistoryCommandService adminHistoryCommandService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("admin-service-command is healthy");
    }

    @PostMapping("/upsert")
    public ResponseEntity<AdminHistoryCommandResponse> upsert(@RequestBody AdminHistoryCommandRequest request) {
        return ResponseEntity.ok(adminHistoryCommandService.upsert(request));
    }
}
