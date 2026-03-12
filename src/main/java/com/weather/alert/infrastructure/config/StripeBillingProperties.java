package com.weather.alert.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.billing.stripe")
@Data
public class StripeBillingProperties {

    private boolean enabled;
    private String secretKey;
    private String webhookSecret;
    private String plusPriceId;
    private String proPriceId;
    private String priceId;
    private String successUrl = "http://localhost:5174/billing/success?session_id={CHECKOUT_SESSION_ID}";
    private String cancelUrl = "http://localhost:5174/billing/cancel";
    private String portalReturnUrl = "http://localhost:5174/app/account";
}
