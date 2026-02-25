package com.weather.alert.infrastructure.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.application.usecase.ManageAlertCriteriaUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.infrastructure.config.SecurityConfig;
import com.weather.alert.infrastructure.web.controller.AlertCriteriaController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertCriteriaController.class)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "app.security.user.username=test-user",
        "app.security.user.password=test-user-password",
        "app.security.admin.username=test-admin",
        "app.security.admin.password=test-admin-password",
        "app.security.jwt.secret=test-jwt-signing-secret-with-minimum-length-123",
        "management.tracing.enabled=false"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ManageAlertCriteriaUseCase manageAlertCriteriaUseCase;

    @MockBean
    private QueryAlertsUseCase queryAlertsUseCase;

    @Test
    void shouldReturnStructuredNotFoundError() throws Exception {
        when(queryAlertsUseCase.getCriteriaById("missing-id"))
                .thenThrow(new CriteriaNotFoundException("missing-id"));

        mockMvc.perform(get("/api/criteria/missing-id")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_HEADER))
                .andExpect(jsonPath("$.errorCode").value("CRITERIA_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/criteria/missing-id"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnValidationErrorForInvalidRequestBody() throws Exception {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .location("Seattle")
                .build();

        mockMvc.perform(post("/api/criteria")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("userId"));
    }

    @Test
    void shouldReturnGlobalValidationErrorsForCrossFieldRules() throws Exception {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("dev-admin")
                .rainThreshold(40.0)
                .build();

        mockMvc.perform(post("/api/criteria")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("rainThreshold and rainThresholdType must be provided together")));
    }
}
