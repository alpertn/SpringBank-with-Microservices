package com.banking_microservices.customer_service_command.service.helper;

import com.banking_microservices.customer_service_command.exception.InvalidCustomerStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class NationalIdProtector {
    private final byte[] hmacKey;

    public NationalIdProtector(@Value("${customer-service.national-id-hmac-key}") String hmacKey) {
        if (hmacKey == null || hmacKey.length() < 32) {
            throw new IllegalArgumentException("customer national-id HMAC key must contain at least 32 characters");
        }
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
    }

    public ProtectedNationalId protect(String nationalId) {
        String normalized = nationalId == null ? "" : nationalId.trim();
        if (!isValidTurkishNationalId(normalized)) {
            throw new InvalidCustomerStateException("nationalId must be a valid 11 digit Turkish national identity number");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            String hash = HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
            return new ProtectedNationalId(hash, "*******" + normalized.substring(7));
        } catch (Exception exception) {
            throw new IllegalStateException("National identity protection could not be initialized", exception);
        }
    }

    boolean isValidTurkishNationalId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{10}")) return false;
        int[] digits = value.chars().map(character -> character - '0').toArray();
        int odd = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int even = digits[1] + digits[3] + digits[5] + digits[7];
        return ((odd * 7 - even) % 10 + 10) % 10 == digits[9]
                && java.util.Arrays.stream(digits, 0, 10).sum() % 10 == digits[10];
    }

    public record ProtectedNationalId(String hash, String masked) {}
}
