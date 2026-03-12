package com.weather.alert.domain.port;

import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.BillingCheckoutSessionRequest;
import com.weather.alert.domain.model.BillingWebhookEvent;

public interface BillingProviderPort {

    BillingCheckoutSession createSubscriptionCheckoutSession(BillingCheckoutSessionRequest request);

    BillingCheckoutSession createCustomerPortalSession(String stripeCustomerId);

    BillingWebhookEvent changeSubscriptionPlan(String stripeSubscriptionId, String newPriceId);

    BillingWebhookEvent cancelSubscription(String stripeSubscriptionId);

    void cancelCustomerBilling(String stripeCustomerId, String stripeSubscriptionId);

    BillingWebhookEvent parseWebhookEvent(String payload, String signatureHeader);
}
