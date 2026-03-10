package com.weather.alert.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weather.alert.domain.model.BillingPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillingStatusResponse {
    private String userId;
    private BillingPlan plan;
    private boolean paidPlan;
    private Integer maxActiveAlerts;
    private Boolean adSponsoredEmails;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;
    private String stripeSubscriptionStatus;
    private Instant stripeCurrentPeriodEnd;
    private boolean activeSubscription;
}
