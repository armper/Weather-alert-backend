package com.weather.alert.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.usecase.ManageAlertCriteriaUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.infrastructure.web.controller.AlertCriteriaController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertCriteriaController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.user.username=test-user",
        "app.security.user.password=test-user-password",
        "app.security.admin.username=test-admin",
        "app.security.admin.password=test-admin-password"
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

    @Test
    void shouldRequireAuthenticationForApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/criteria/criteria-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidCriteriaWriteForNonAdminUser() throws Exception {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .location("Seattle")
                .build();

        mockMvc.perform(post("/api/criteria")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
