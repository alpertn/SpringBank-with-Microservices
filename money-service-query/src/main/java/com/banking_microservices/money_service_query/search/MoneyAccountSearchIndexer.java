package com.banking_microservices.money_service_query.search;

import com.banking_microservices.money_service_query.model.MoneyAccountSearchDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MoneyAccountSearchIndexer {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI elasticsearchBaseUri;

    public MoneyAccountSearchIndexer(
            @Value("${spring.elasticsearch.uris:http://elasticsearch:9200}") String elasticsearchUris
    ) {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.elasticsearchBaseUri = URI.create(firstUri(elasticsearchUris));
    }

    public void upsert(MoneyAccountSearchDocument document) {
        HttpRequest request = HttpRequest.newBuilder(documentUri(document.getId()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(asJson(document)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!HttpStatus.valueOf(response.statusCode()).is2xxSuccessful()) {
                throw new IllegalStateException("Elasticsearch upsert failed with status=" + response.statusCode() + ", body=" + response.body());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch upsert IO failure for id=" + document.getId(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch upsert interrupted for id=" + document.getId(), exception);
        }
    }

    private URI documentUri(String id) {
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
        return elasticsearchBaseUri.resolve("/money-accounts/_doc/" + encodedId + "?refresh=true");
    }

    private String asJson(MoneyAccountSearchDocument document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", document.getUserId());
        payload.put("keycloakUserUUID", document.getKeycloakUserUUID());
        payload.put("userIban", document.getUserIban());
        payload.put("availableBalance", document.getAvailableBalance());
        payload.put("blockedBalance", document.getBlockedBalance());
        payload.put("lastOperationType", document.getLastOperationType());
        payload.put("lastSyncedAt", document.getLastSyncedAt() == null ? null : document.getLastSyncedAt().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Elasticsearch payload serialization failed for id=" + document.getId(), exception);
        }
    }

    private static String firstUri(String elasticsearchUris) {
        String[] candidates = elasticsearchUris.split(",");
        return candidates[0].trim();
    }
}
