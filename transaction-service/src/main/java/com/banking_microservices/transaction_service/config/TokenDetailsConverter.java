package com.banking_microservices.transaction_service.config;

import com.banking_microservices.transaction_service.dto.TokenDetailsDto;
import com.google.gson.Gson;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TokenDetailsConverter implements AttributeConverter<TokenDetailsDto, String> {

    private static final Gson GSON = new Gson();

    @Override
    public String convertToDatabaseColumn(TokenDetailsDto attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return GSON.toJson(attribute);
        } catch (Exception exception) {
            throw new IllegalArgumentException("TokenDetailsDto serialize edilemedi.", exception);
        }
    }

    @Override
    public TokenDetailsDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return GSON.fromJson(dbData, TokenDetailsDto.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("TokenDetailsDto deserialize edilemedi.", exception);
        }
    }
}
