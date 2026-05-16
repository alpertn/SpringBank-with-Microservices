package com.banking_microservices.user_service.config;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class KeycloakBootstrapSetup {

    private static final AtomicBoolean RAN = new AtomicBoolean(false);

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    @Value("${keycloak.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${keycloak.bootstrap.wait-timeout-seconds:180}")
    private long waitTimeoutSeconds;

    @Value("${keycloak.bootstrap.test-user-email:testuser@bank.com}")
    private String testUserEmail;

    @Value("${keycloak.bootstrap.test-user-password:pass123}")
    private String testUserPassword;

    @Value("${keycloak.bootstrap.admin-user-email:admin@gmail.com}")
    private String defaultAdminEmail;

    @Value("${keycloak.bootstrap.admin-user-password:admin123}")
    private String defaultAdminPassword;

    public void setupOnce() {
        if (!bootstrapEnabled) {
            log.info("Keycloak bootstrap disabled.");
            return;
        }
        if (!RAN.compareAndSet(false, true)) {
            log.info("Keycloak bootstrap already called in this process.");
            return;
        }

        waitUntilKeycloakReady();

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm("master")
                .username(adminUsername)
                .password(adminPassword)
                .clientId("admin-cli")
                .build()) {

            createOrUpdateRealm(keycloak);
            createRoleIfMissing(keycloak, "USER");
            createRoleIfMissing(keycloak, "ADMIN");
            createOrUpdateClient(keycloak);
            String testUserId = createOrUpdateUser(keycloak, testUserEmail, "Test", "User", testUserPassword);
            String adminUserId = createOrUpdateUser(keycloak, defaultAdminEmail, "Admin", "User", defaultAdminPassword);
            assignRealmRole(keycloak, testUserId, "USER");
            assignRealmRole(keycloak, adminUserId, "ADMIN");

            log.info("Keycloak bootstrap completed. Admin: {} / configured password, test user: {}",
                    defaultAdminEmail, testUserEmail);
        } catch (Exception exception) {
            log.error("Keycloak bootstrap failed: {}", exception.getMessage(), exception);
        }
    }

    private void waitUntilKeycloakReady() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        URI uri = URI.create(trimTrailingSlash(keycloakUrl) + "/realms/master");
        long deadline = System.nanoTime() + Duration.ofSeconds(waitTimeoutSeconds).toNanos();

        while (System.nanoTime() < deadline) {
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                if (status >= 200 && status < 500) {
                    log.info("Keycloak is ready at {}", uri);
                    return;
                }
            } catch (Exception exception) {
                log.info("Keycloak not ready yet at {}: {}", uri, exception.getMessage());
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Keycloak", interruptedException);
            }
        }

        throw new IllegalStateException("Keycloak did not become ready within " + waitTimeoutSeconds + " seconds");
    }

    private void createOrUpdateRealm(Keycloak keycloak) {
        RealmRepresentation representation = new RealmRepresentation();
        representation.setRealm(realm);
        representation.setEnabled(true);
        representation.setAccessTokenLifespan(600);
        representation.setSsoSessionIdleTimeout(3600);
        representation.setSsoSessionMaxLifespan(72000);

        try {
            keycloak.realm(realm).toRepresentation();
            keycloak.realm(realm).update(representation);
            log.info("Keycloak realm updated: {}", realm);
        } catch (NotFoundException exception) {
            keycloak.realms().create(representation);
            log.info("Keycloak realm created: {}", realm);
        }
    }

    private void createRoleIfMissing(Keycloak keycloak, String roleName) {
        try {
            keycloak.realm(realm).roles().get(roleName).toRepresentation();
            log.info("Keycloak role already exists: {}", roleName);
        } catch (NotFoundException exception) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            keycloak.realm(realm).roles().create(role);
            log.info("Keycloak role created: {}", roleName);
        }
    }

    private void createOrUpdateClient(Keycloak keycloak) {
        List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientId);
        ClientRepresentation client = clients == null || clients.isEmpty()
                ? new ClientRepresentation()
                : clients.getFirst();

        client.setClientId(clientId);
        client.setSecret(clientSecret);
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setStandardFlowEnabled(true);

        if (client.getId() == null) {
            keycloak.realm(realm).clients().create(client);
            log.info("Keycloak client created: {}", clientId);
        } else {
            keycloak.realm(realm).clients().get(client.getId()).update(client);
            log.info("Keycloak client updated: {}", clientId);
        }
    }

    private String createOrUpdateUser(Keycloak keycloak, String email, String firstName, String lastName, String password) {
        List<UserRepresentation> existingUsers = keycloak.realm(realm).users().searchByEmail(email, true);
        UserRepresentation user = existingUsers == null ? null : existingUsers.stream()
                .filter(candidate -> email.equalsIgnoreCase(candidate.getEmail()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true);
            user.setCredentials(Collections.singletonList(passwordCredential(password)));

            try (Response response = keycloak.realm(realm).users().create(user)) {
                if (response.getStatus() != 201) {
                    throw new IllegalStateException("Keycloak user create failed for " + email + ": "
                            + response.readEntity(String.class));
                }
                String location = response.getHeaderString("Location");
                String id = location.substring(location.lastIndexOf('/') + 1);
                log.info("Keycloak user created: {}", email);
                return id;
            }
        }

        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);
        keycloak.realm(realm).users().get(user.getId()).update(user);
        keycloak.realm(realm).users().get(user.getId()).resetPassword(passwordCredential(password));
        log.info("Keycloak user updated: {}", email);
        return user.getId();
    }

    private void assignRealmRole(Keycloak keycloak, String userId, String roleName) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required for role " + roleName);
        }
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        List<RoleRepresentation> currentRoles = keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .listEffective();

        boolean alreadyAssigned = currentRoles.stream()
                .map(RoleRepresentation::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> name.equalsIgnoreCase(roleName));

        if (!alreadyAssigned) {
            keycloak.realm(realm).users().get(userId).roles().realmLevel().add(List.of(role));
            log.info("Keycloak role {} assigned to user {}", roleName, userId);
        }
    }

    private CredentialRepresentation passwordCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
