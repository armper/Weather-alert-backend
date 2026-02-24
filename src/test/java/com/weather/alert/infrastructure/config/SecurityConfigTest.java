package com.weather.alert.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.application.dto.AuthRequest;
import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.usecase.ManageAlertCriteriaUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.infrastructure.web.controller.AlertCriteriaController;
import com.weather.alert.infrastructure.web.controller.AuthController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AlertCriteriaController.class, AuthController.class})
@Import(SecurityConfig.class)
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
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtEncoder jwtEncoder;

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
    void shouldForbidCriteriaWriteForNonAdminUser() throws Exception {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .location("Seattle")
                .build();

        mockMvc.perform(post("/api/criteria")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
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
                .andExpect(status().isOk());
    }

    @Test
    void shouldIssueJwtTokenForValidCredentials() throws Exception {
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
}
