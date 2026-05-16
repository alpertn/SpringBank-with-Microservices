package com.banking_microservices.user_service.grpc;

import com.banking_microservices.user_service.exception.InvalidTokenException;
import com.banking_microservices.user_service.service.TokenDecoderService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class AuthTokenGrpcEndpoint extends AuthTokenGrpcServiceGrpc.AuthTokenGrpcServiceImplBase {

    private final TokenDecoderService tokenDecoderService;

    @Override
    public void decodeToken(TokenDecodeRequest request, StreamObserver<TokenDetailsResponse> responseObserver) {
        try {
            TokenDecoderService.DecodedTokenDetails tokenDetails = tokenDecoderService.decode(request.getToken());
            responseObserver.onNext(TokenDetailsResponse.newBuilder()
                    .setSubject(tokenDetails.getSubject())
                    .setPreferredUsername(tokenDetails.getPreferredUsername())
                    .setEmail(tokenDetails.getEmail())
                    .setGivenName(tokenDetails.getGivenName())
                    .setFamilyName(tokenDetails.getFamilyName())
                    .setIssuer(tokenDetails.getIssuer())
                    .setAudienceJson(tokenDetails.getAudienceJson())
                    .addAllRealmRoles(tokenDetails.getRealmRoles())
                    .setHeaderJson(tokenDetails.getHeaderJson())
                    .setClaimsJson(tokenDetails.getClaimsJson())
                    .setIssuedAt(tokenDetails.getIssuedAt())
                    .setExpiresAt(tokenDetails.getExpiresAt())
                    .setJwtId(tokenDetails.getJwtId())
                    .setTokenType(tokenDetails.getTokenType())
                    .setSessionState(tokenDetails.getSessionState())
                    .setScope(tokenDetails.getScope())
                    .build());
            responseObserver.onCompleted();
        } catch (InvalidTokenException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("user-service token decode failed")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
