package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingWebhookEvent {
    private BillingWebhookEventType type;
    private String userId;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;
    private String stripeSubscriptionStatus;
    private Instant stripeCurrentPeriodEnd;
}
