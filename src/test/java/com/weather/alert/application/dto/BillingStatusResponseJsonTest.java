package com.weather.alert.application.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.model.BillingPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingStatusResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldOmitNullStripeFieldsFromJson() throws Exception {
        BillingStatusResponse response = BillingStatusResponse.builder()
                .userId("user-1")
                .plan(BillingPlan.FREE)
                .maxActiveAlerts(1)
                .maxTravelPlans(0)
                .adSponsoredEmails(true)
                .activeSubscription(false)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"userId\":\"user-1\""));
        assertTrue(json.contains("\"plan\":\"FREE\""));
        assertTrue(json.contains("\"maxActiveAlerts\":1"));
        assertTrue(json.contains("\"maxTravelPlans\":0"));
        assertTrue(json.contains("\"adSponsoredEmails\":true"));
        assertTrue(json.contains("\"activeSubscription\":false"));
        assertFalse(json.contains("stripeCustomerId"));
        assertFalse(json.contains("stripeCurrentPeriodEnd"));
    }
}
