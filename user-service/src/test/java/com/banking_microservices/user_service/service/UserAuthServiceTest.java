package com.banking_microservices.user_service.service;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum.Role;
import com.banking_microservices.user_service.dto.auth.LoginRequestDto;
import com.banking_microservices.user_service.dto.auth.RefleshTokenRequestDto;
import com.banking_microservices.user_service.dto.auth.RegisterDto;
import com.banking_microservices.user_service.dto.auth.TokenResponseDto;
import com.banking_microservices.user_service.kafka.KafkaSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private KafkaSender kafkaSender;

    @InjectMocks
    private UserAuthService userAuthService;

    @Test
    void registerCreatesKeycloakUserAndPublishesCreateUserTopic() {
        RegisterDto request = RegisterDto.builder()
                .email("sender@springbank.test")
                .password("Test1234!")
                .name("Sender")
                .surname("User")
                .build();
        when(keycloakAdminService.createUser(request, Role.USER)).thenReturn("keycloak-uuid-1");

        userAuthService.register(request);

        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaSender).sendCreateUser(userIdCaptor.capture());
        assertThat(userIdCaptor.getValue()).isEqualTo("keycloak-uuid-1");
    }

    @Test
    void loginDelegatesToKeycloakUserService() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("sender@springbank.test")
                .password("Test1234!")
                .build();
        TokenResponseDto response = TokenResponseDto.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(300L)
                .build();
        when(keycloakUserService.login(request)).thenReturn(response);

        assertThat(userAuthService.login(request)).isSameAs(response);
    }

    @Test
    void refreshAndLogoutUseRefreshToken() {
        RefleshTokenRequestDto request = RefleshTokenRequestDto.builder()
                .refreshToken("refresh-token")
                .build();
        TokenResponseDto response = TokenResponseDto.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(300L)
                .build();
        when(keycloakUserService.refreshWithRefreshToken("refresh-token")).thenReturn(response);

        assertThat(userAuthService.refresh(request)).isSameAs(response);
        userAuthService.logout(request);

        verify(keycloakUserService).logout("refresh-token");
    }
}
