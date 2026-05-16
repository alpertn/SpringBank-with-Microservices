package com.banking_microservices.transaction_service.grpc;

import com.banking_microservices.user_service.grpc.AuthTokenGrpcServiceGrpc;
import com.banking_microservices.user_service.grpc.TokenDecodeRequest;
import com.banking_microservices.user_service.grpc.TokenDetailsResponse;
import com.banking_microservices.transaction_service.dto.TokenDetailsDto;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenGrpcClient {

    @GrpcClient("user-service-token")
    private AuthTokenGrpcServiceGrpc.AuthTokenGrpcServiceBlockingStub authTokenGrpcServiceBlockingStub;

    public TokenDetailsDto decodeToken(String token) {
        try {
            TokenDetailsResponse response = authTokenGrpcServiceBlockingStub.decodeToken(
                    TokenDecodeRequest.newBuilder()
                            .setToken(token == null ? "" : token)
                            .build());

            return TokenDetailsDto.builder()
                    .subject(response.getSubject())
                    .preferredUsername(response.getPreferredUsername())
                    .email(response.getEmail())
                    .givenName(response.getGivenName())
                    .familyName(response.getFamilyName())
                    .issuer(response.getIssuer())
                    .audienceJson(response.getAudienceJson())
                    .realmRoles(response.getRealmRolesList())
                    .headerJson(response.getHeaderJson())
                    .claimsJson(response.getClaimsJson())
                    .issuedAt(response.getIssuedAt())
                    .expiresAt(response.getExpiresAt())
                    .jwtId(response.getJwtId())
                    .tokenType(response.getTokenType())
                    .sessionState(response.getSessionState())
                    .scope(response.getScope())
                    .build();
        } catch (StatusRuntimeException exception) {
            throw new IllegalArgumentException("user-service token decode hatasi: " + exception.getStatus().getDescription(), exception);
        }
    }
}
