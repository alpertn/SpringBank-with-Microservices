package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.exception.InvalidTokenException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class TokenDecoderService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public TokenDecoderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DecodedTokenDetails decode(String rawToken) {
        String token = normalize(rawToken);
        String[] tokenParts = token.split("\\.");

        if (tokenParts.length < 2) {
            throw new InvalidTokenException("Token format gecersiz.");
        }

        try {
            Map<String, Object> header = parseJsonPart(tokenParts[0]);
            Map<String, Object> claims = parseJsonPart(tokenParts[1]);

            return DecodedTokenDetails.builder()
                    .subject(getString(claims, "sub"))
                    .preferredUsername(getString(claims, "preferred_username"))
                    .email(getString(claims, "email"))
                    .givenName(getString(claims, "given_name"))
                    .familyName(getString(claims, "family_name"))
                    .issuer(getString(claims, "iss"))
                    .audienceJson(writeJson(claims.get("aud")))
                    .realmRoles(extractRealmRoles(claims))
                    .headerJson(writeJson(header))
                    .claimsJson(writeJson(claims))
                    .issuedAt(getStringValue(claims.get("iat")))
                    .expiresAt(getStringValue(claims.get("exp")))
                    .jwtId(getString(claims, "jti"))
                    .tokenType(getString(claims, "typ"))
                    .sessionState(getString(claims, "session_state"))
                    .scope(getString(claims, "scope"))
                    .build();
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException("Token parse edilemedi: " + exception.getMessage());
        }
    }

    private Map<String, Object> parseJsonPart(String encodedPart) throws Exception {
        byte[] decoded = Base64.getUrlDecoder().decode(encodedPart);
        return objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), MAP_TYPE);
    }

    private String normalize(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Token bos olamaz.");
        }

        return rawToken.startsWith("Bearer ") ? rawToken.substring(7).trim() : rawToken.trim();
    }

    private String getString(Map<String, Object> claims, String key) {
        return getStringValue(claims.get(key));
    }

    private String getStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String writeJson(Object value) {
        try {
            return value == null ? "" : objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new InvalidTokenException("JSON serialize hatasi: " + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            return Collections.emptyList();
        }

        Object roles = realmMap.get("roles");
        if (!(roles instanceof List<?> roleList)) {
            return Collections.emptyList();
        }

        return roleList.stream().map(String::valueOf).toList();
    }

    @Getter
    @Builder
    public static class DecodedTokenDetails {
        private final String subject;
        private final String preferredUsername;
        private final String email;
        private final String givenName;
        private final String familyName;
        private final String issuer;
        private final String audienceJson;
        private final List<String> realmRoles;
        private final String headerJson;
        private final String claimsJson;
        private final String issuedAt;
        private final String expiresAt;
        private final String jwtId;
        private final String tokenType;
        private final String sessionState;
        private final String scope;
    }
}
