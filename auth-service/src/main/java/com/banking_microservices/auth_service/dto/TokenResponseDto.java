package com.banking_microservices.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDto {
    // TODO: @JsonProperty Jsondan gelen snake-case veriyi Java icin uygun olan camelCase'ye cevırıyor. zorunlu mu? Değil. Ama Standartlara uymak icin ekledim.
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
