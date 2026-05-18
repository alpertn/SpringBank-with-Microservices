package com.banking_microservices.admin_service_query.search;

import com.banking_microservices.admin_service_query.model.AdminHistorySearchDocument;
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
public class AdminHistorySearchIndexer {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI elasticsearchBaseUri;

    public AdminHistorySearchIndexer(
            @Value("${spring.elasticsearch.uris:http://elasticsearch:9200}") String elasticsearchUris
    ) {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.elasticsearchBaseUri = URI.create(firstUri(elasticsearchUris));
    }

    public void upsert(AdminHistorySearchDocument document) {
        HttpRequest request = HttpRequest.newBuilder(documentUri(document.getRequestId()))
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
            throw new IllegalStateException("Elasticsearch upsert IO failure for requestId=" + document.getRequestId(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch upsert interrupted for requestId=" + document.getRequestId(), exception);
        }
    }

    private URI documentUri(String id) {
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
        return elasticsearchBaseUri.resolve("/admin-query-history/_doc/" + encodedId + "?refresh=true");
    }

    private String asJson(AdminHistorySearchDocument document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("adminEmail", document.getAdminEmail());
        payload.put("transport", document.getTransport());
        payload.put("requestType", document.getRequestType());
        payload.put("targetType", document.getTargetType());
        payload.put("targetName", document.getTargetName());
        payload.put("topicName", document.getTopicName());
        payload.put("status", document.getStatus());
        payload.put("responseType", document.getResponseType());
        payload.put("queryText", document.getQueryText());
        payload.put("requestPayload", document.getRequestPayload());
        payload.put("responsePayload", document.getResponsePayload());
        payload.put("errorMessage", document.getErrorMessage());
        payload.put("requestedAt", document.getRequestedAt() == null ? null : document.getRequestedAt().toString());
        payload.put("receivedAt", document.getReceivedAt() == null ? null : document.getReceivedAt().toString());
        payload.put("lastSyncedAt", document.getLastSyncedAt() == null ? null : document.getLastSyncedAt().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Elasticsearch payload serialization failed for requestId=" + document.getRequestId(), exception);
        }
    }

    private static String firstUri(String elasticsearchUris) {
        return elasticsearchUris.split(",")[0].trim();
    }
}
