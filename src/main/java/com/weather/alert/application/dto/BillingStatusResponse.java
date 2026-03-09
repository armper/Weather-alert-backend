package com.weather.alert.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingStatusResponse {
    private String userId;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;
    private String stripeSubscriptionStatus;
    private Instant stripeCurrentPeriodEnd;
    private boolean activeSubscription;
}
