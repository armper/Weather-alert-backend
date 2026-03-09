package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingCheckoutSessionRequest {
    private String userId;
    private String email;
    private String stripeCustomerId;
}
