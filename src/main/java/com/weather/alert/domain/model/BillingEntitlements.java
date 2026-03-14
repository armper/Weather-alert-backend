package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingEntitlements {
    private BillingPlan plan;
    private boolean paidPlan;
    private int maxActiveAlerts;
    private int maxTravelPlans;
    private boolean adSponsoredEmails;
}
