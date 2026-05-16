package com.banking_microservices.fraud_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDetailsDto {

    private String subject;
    private String preferredUsername;
    private String email;
    private String givenName;
    private String familyName;
    private String issuer;
    private String audienceJson;
    private List<String> realmRoles;
    private String headerJson;
    private String claimsJson;
    private String issuedAt;
    private String expiresAt;
    private String jwtId;
    private String tokenType;
    private String sessionState;
    private String scope;
}
