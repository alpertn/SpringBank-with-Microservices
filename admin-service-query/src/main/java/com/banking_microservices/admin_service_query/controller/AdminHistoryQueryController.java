package com.banking_microservices.admin_service_query.controller;

import com.banking_microservices.admin_service_query.dto.AdminHistoryReadDto;
import com.banking_microservices.admin_service_query.service.AdminHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-service-query/v1/history")
@RequiredArgsConstructor
public class AdminHistoryQueryController {

    private final AdminHistoryQueryService adminHistoryQueryService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("admin-service-query is healthy");
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<AdminHistoryReadDto> getByRequestId(@PathVariable String requestId) {
        return ResponseEntity.ok(adminHistoryQueryService.getByRequestId(requestId));
    }

    @GetMapping
    public ResponseEntity<List<AdminHistoryReadDto>> list(
            @RequestParam(defaultValue = "40") int limit,
            @RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(adminHistoryQueryService.list(limit, keyword));
    }
}
