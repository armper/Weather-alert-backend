package com.weather.alert.domain.port;

import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.BillingCheckoutSessionRequest;
import com.weather.alert.domain.model.BillingWebhookEvent;

public interface BillingProviderPort {

    BillingCheckoutSession createSubscriptionCheckoutSession(BillingCheckoutSessionRequest request);

    BillingWebhookEvent parseWebhookEvent(String payload, String signatureHeader);
}
