package com.weather.alert.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.application.dto.AuthRequest;
import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.usecase.AuthenticateRegisteredUserUseCase;
import com.weather.alert.application.usecase.CreateBillingCheckoutSessionUseCase;
import com.weather.alert.application.usecase.CreateBillingPortalSessionUseCase;
import com.weather.alert.application.usecase.GetBillingStatusUseCase;
import com.weather.alert.application.usecase.HandleStripeWebhookUseCase;
import com.weather.alert.application.usecase.ManageAccountRecoveryUseCase;
import com.weather.alert.application.usecase.ManageAlertCriteriaUseCase;
import com.weather.alert.application.usecase.ManageNotificationPreferencesUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.application.service.AuthSecurityGuardService;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.infrastructure.error.CorrelationIdFilter;
import com.weather.alert.infrastructure.error.RestAccessDeniedHandler;
import com.weather.alert.infrastructure.error.RestAuthenticationEntryPoint;
import com.weather.alert.infrastructure.error.SecurityErrorResponseWriter;
import com.weather.alert.infrastructure.web.controller.AlertCriteriaController;
import com.weather.alert.infrastructure.web.controller.AccountRecoveryController;
import com.weather.alert.infrastructure.web.controller.AuthController;
import com.weather.alert.infrastructure.web.controller.BillingController;
import com.weather.alert.infrastructure.web.controller.NotificationPreferenceController;
import com.weather.alert.infrastructure.web.controller.StripeWebhookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AlertCriteriaController.class,
        AuthController.class,
        NotificationPreferenceController.class,
        AccountRecoveryController.class,
        BillingController.class,
        StripeWebhookController.class
})
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class
})
@TestPropertySource(properties = {
        "app.security.user.username=test-user",
        "app.security.user.password=test-user-password",
        "app.security.admin.username=test-admin",
        "app.security.admin.password=test-admin-password",
        "app.security.jwt.secret=test-jwt-signing-secret-with-minimum-length-123"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ManageAlertCriteriaUseCase manageAlertCriteriaUseCase;

    @MockBean
    private QueryAlertsUseCase queryAlertsUseCase;

    @MockBean
    private ManageNotificationPreferencesUseCase manageNotificationPreferencesUseCase;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtEncoder jwtEncoder;

    @MockBean
    private AuthenticateRegisteredUserUseCase authenticateRegisteredUserUseCase;

    @MockBean
    private AuthSecurityGuardService authSecurityGuardService;

    @MockBean
    private ManageAccountRecoveryUseCase manageAccountRecoveryUseCase;

    @MockBean
    private GetBillingStatusUseCase getBillingStatusUseCase;

    @MockBean
    private CreateBillingCheckoutSessionUseCase createBillingCheckoutSessionUseCase;

    @MockBean
    private CreateBillingPortalSessionUseCase createBillingPortalSessionUseCase;

    @MockBean
    private HandleStripeWebhookUseCase handleStripeWebhookUseCase;

    @Test
    void shouldRequireAuthenticationForApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/criteria/criteria-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowSwaggerEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowCriteriaWriteForUserRoleWhenUserOwnsCriteria() throws Exception {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-1")
                .userId("user-1")
                .enabled(true)
                .build();
        when(manageAlertCriteriaUseCase.createCriteria(any(CreateAlertCriteriaRequest.class))).thenReturn(criteria);

        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .location("Seattle")
                .build();

        mockMvc.perform(post("/api/criteria")
                        .with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("criteria-1"));
    }

    @Test
    void shouldForbidCriteriaWriteWhenNonAdminSpoofsAnotherUserId() throws Exception {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("other-user")
                .location("Seattle")
                .build();

        mockMvc.perform(post("/api/criteria")
                        .with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidCriteriaReadForAnotherUserWhenRequesterIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/criteria/user/other-user")
                        .with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowCriteriaWriteForAdminUser() throws Exception {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-1")
                .userId("user-1")
                .enabled(true)
                .build();
        when(manageAlertCriteriaUseCase.createCriteria(any(CreateAlertCriteriaRequest.class))).thenReturn(criteria);

        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .location("Seattle")
                .build();

        mockMvc.perform(post("/api/criteria")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("criteria-1"))
                .andExpect(jsonPath("$.eventType").doesNotExist());
    }

    @Test
    void shouldAllowUserToUpdateOwnNotificationPreferences() throws Exception {
        when(manageNotificationPreferencesUseCase.upsertUserPreference(any(), any()))
                .thenReturn(com.weather.alert.domain.model.UserNotificationPreference.builder()
                        .userId("user-1")
                        .enabledChannels(List.of(NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.EMAIL)
                        .fallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS)
                        .build());

        mockMvc.perform(put("/api/users/me/notification-preferences")
                        .with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content("""
                                {
                                  "enabledChannels": ["EMAIL"],
                                  "preferredChannel": "EMAIL",
                                  "fallbackStrategy": "FIRST_SUCCESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.preferredChannel").value("EMAIL"));
    }

    @Test
    void shouldForbidCriteriaPreferenceReadForNonOwner() throws Exception {
        when(queryAlertsUseCase.getCriteriaById("criteria-1")).thenReturn(AlertCriteria.builder()
                .id("criteria-1")
                .userId("owner-user")
                .enabled(true)
                .build());

        mockMvc.perform(get("/api/criteria/criteria-1/notification-preferences")
                        .with(jwt().jwt(jwt -> jwt.subject("other-user")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldIssueJwtTokenForValidCredentials() throws Exception {
        when(authenticateRegisteredUserUseCase.authenticate(any(), any())).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "test-user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(new Jwt(
                "jwt-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "test-user", "scope", "ROLE_USER")));

        AuthRequest request = new AuthRequest();
        request.setUsername("test-user");
        request.setPassword("test-user-password");

        mockMvc.perform(post("/api/auth/token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldAllowRecoveryRequestWithoutAuthentication() throws Exception {
        when(manageAccountRecoveryUseCase.requestUsernameReminder(any(), any()))
                .thenReturn(com.weather.alert.application.dto.RecoveryRequestResponse.builder()
                        .message("If an account exists, recovery instructions were sent.")
                        .recoveryId("recovery-1")
                        .build());

        mockMvc.perform(post("/api/auth/recovery/username/request")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryId").value("recovery-1"));
    }

    @Test
    void shouldAllowPasswordRecoveryRequestWithoutAuthentication() throws Exception {
        when(manageAccountRecoveryUseCase.requestPasswordReset(any(), any()))
                .thenReturn(com.weather.alert.application.dto.RecoveryRequestResponse.builder()
                        .message("If an account exists, recovery instructions were sent.")
                        .recoveryId("recovery-password-1")
                        .build());

        mockMvc.perform(post("/api/auth/recovery/password/request")
                        .contentType("application/json")
                        .content("""
                                {
                                  "usernameOrEmail": "user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryId").value("recovery-password-1"));
    }

    @Test
    void shouldAllowPasswordRecoveryConfirmWithoutAuthentication() throws Exception {
        when(manageAccountRecoveryUseCase.confirmPasswordReset(any(), any()))
                .thenReturn(com.weather.alert.application.dto.MessageResponse.builder()
                        .message("Password updated successfully.")
                        .build());

        mockMvc.perform(post("/api/auth/recovery/password/confirm")
                        .contentType("application/json")
                        .content("""
                                {
                                  "recoveryId": "4f5f913d-baa8-4d20-8f72-e894712b8b23",
                                  "code": "A2B3C4D5",
                                  "newPassword": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully."));
    }

    @Test
    void shouldRequireAuthenticationForBillingStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/billing/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedUserToReadBillingStatus() throws Exception {
        when(getBillingStatusUseCase.getForUser("user-1")).thenReturn(BillingStatusResponse.builder()
                .userId("user-1")
                .stripeSubscriptionStatus("active")
                .activeSubscription(true)
                .build());

        mockMvc.perform(get("/api/billing/me")
                        .with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.activeSubscription").value(true));
    }

    @Test
    void shouldAllowAuthenticatedUserToCreateBillingCheckoutSession() throws Exception {
        when(createBillingCheckoutSessionUseCase.createForUser("user-1", null)).thenReturn(BillingCheckoutSessionResponse.builder()
                .sessionId("cs_test_123")
                .url("https://checkout.stripe.com/c/pay/cs_test_123")
                .build());

        mockMvc.perform(post("/api/billing/checkout-session")
                        .contentType("application/json")
                        .content("{}")
                        .with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("cs_test_123"));
    }

    @Test
    void shouldAllowAuthenticatedUserToCreateBillingPortalSession() throws Exception {
        when(createBillingPortalSessionUseCase.createForUser("user-1")).thenReturn(BillingCheckoutSessionResponse.builder()
                .sessionId("bps_test_123")
                .url("https://billing.stripe.com/p/session/bps_test_123")
                .build());

        mockMvc.perform(post("/api/billing/portal-session")
                        .with(jwt().jwt(jwt -> jwt.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("bps_test_123"));
    }

    @Test
    void shouldAllowStripeWebhookWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .header("Stripe-Signature", "t=1,v1=test")
                        .contentType("application/json")
                        .content("{\"id\":\"evt_123\",\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Webhook processed"));
    }
}
