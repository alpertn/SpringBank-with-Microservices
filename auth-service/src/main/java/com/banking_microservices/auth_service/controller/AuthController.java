package com.banking_microservices.auth_service.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

public class AuthController {
    private final WebClient webClient = WebClient.create();
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

}
