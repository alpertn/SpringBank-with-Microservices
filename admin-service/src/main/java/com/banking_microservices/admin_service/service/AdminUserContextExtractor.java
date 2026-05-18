package com.banking_microservices.admin_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class AdminUserContextExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> extract(String authorizationHeader) {
        String fallback = "unknown-admin";
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Map.of("adminEmail", fallback, "adminPasswordMasked", "NOT_STORED");
        }
        try {
            String token = authorizationHeader.substring("Bearer ".length());
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Map.of("adminEmail", fallback, "adminPasswordMasked", "NOT_STORED");
            }
            byte[] decoded = Base64.getUrlDecoder().decode(pad(parts[1]));
            JsonNode payload = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            String email = firstNonBlank(
                    payload.path("email").asText(""),
                    payload.path("preferred_username").asText(""),
                    payload.path("sub").asText(fallback)
            );
            return Map.of("adminEmail", email, "adminPasswordMasked", "NOT_STORED");
        } catch (Exception ignored) {
            return Map.of("adminEmail", fallback, "adminPasswordMasked", "NOT_STORED");
        }
    }

    private String pad(String input) {
        int remainder = input.length() % 4;
        if (remainder == 0) {
            return input;
        }
        return input + "=".repeat(4 - remainder);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown-admin";
    }
}
