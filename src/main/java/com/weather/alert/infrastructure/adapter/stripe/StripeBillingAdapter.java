package com.weather.alert.infrastructure.adapter.stripe;

import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.application.exception.BillingNotConfiguredException;
import com.weather.alert.application.exception.StripeBillingException;
import com.weather.alert.application.exception.StripeWebhookException;
import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.BillingCheckoutSessionRequest;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.BillingWebhookEventType;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.infrastructure.config.StripeBillingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StripeBillingAdapter implements BillingProviderPort {

    private final StripeBillingProperties properties;

    @Override
    public BillingCheckoutSession createSubscriptionCheckoutSession(BillingCheckoutSessionRequest request) {
        requireCheckoutConfigured(request);
        try {
            Stripe.apiKey = properties.getSecretKey();

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(properties.getSuccessUrl())
                    .setCancelUrl(properties.getCancelUrl())
                    .setClientReferenceId(request.getUserId())
                    .putMetadata("userId", request.getUserId())
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("userId", request.getUserId())
                            .build())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(request.getPriceId())
                            .setQuantity(1L)
                            .build());

            if (request.getStripeCustomerId() != null && !request.getStripeCustomerId().isBlank()) {
                builder.setCustomer(request.getStripeCustomerId());
            } else {
                builder.setCustomerEmail(request.getEmail());
            }

            Session session = Session.create(builder.build());
            return BillingCheckoutSession.builder()
                    .id(session.getId())
                    .url(session.getUrl())
                    .build();
        } catch (StripeException ex) {
            throw new StripeBillingException("Unable to create Stripe Checkout session", ex);
        }
    }

    @Override
    public BillingCheckoutSession createCustomerPortalSession(String stripeCustomerId) {
        requirePortalConfigured(stripeCustomerId);
        try {
            Stripe.apiKey = properties.getSecretKey();

            com.stripe.model.billingportal.Session session = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setReturnUrl(properties.getPortalReturnUrl())
                    .build());

            return BillingCheckoutSession.builder()
                    .id(session.getId())
                    .url(session.getUrl())
                    .build();
        } catch (StripeException ex) {
            throw new StripeBillingException("Unable to create Stripe Customer Portal session", ex);
        }
    }

    @Override
    public BillingWebhookEvent changeSubscriptionPlan(String stripeSubscriptionId, String newPriceId) {
        requireSubscriptionMutationConfigured(stripeSubscriptionId, newPriceId);
        try {
            Stripe.apiKey = properties.getSecretKey();

            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
            String subscriptionItemId = resolvePrimarySubscriptionItemId(subscription);
            Subscription updatedSubscription = subscription.update(SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(false)
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscriptionItemId)
                            .setPrice(newPriceId)
                            .build())
                    .build());

            return fromSubscription(BillingWebhookEventType.SUBSCRIPTION_UPDATED, updatedSubscription);
        } catch (StripeException ex) {
            throw new StripeBillingException("Unable to change Stripe subscription plan", ex);
        }
    }

    @Override
    public BillingWebhookEvent cancelSubscription(String stripeSubscriptionId) {
        requireSubscriptionMutationConfigured(stripeSubscriptionId, "cancel");
        try {
            Stripe.apiKey = properties.getSecretKey();

            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
            Subscription canceledSubscription = subscription;
            String status = subscription == null ? null : subscription.getStatus();
            if (status != null
                    && !"canceled".equalsIgnoreCase(status)
                    && !"incomplete_expired".equalsIgnoreCase(status)) {
                canceledSubscription = subscription.cancel();
            }

            return fromSubscription(BillingWebhookEventType.SUBSCRIPTION_DELETED, canceledSubscription);
        } catch (StripeException ex) {
            throw new StripeBillingException("Unable to cancel Stripe subscription", ex);
        }
    }

    @Override
    public void cancelCustomerBilling(String stripeCustomerId, String stripeSubscriptionId) {
        if (!properties.isEnabled() || isBlank(properties.getSecretKey())) {
            return;
        }

        try {
            Stripe.apiKey = properties.getSecretKey();

            if (!isBlank(stripeSubscriptionId)) {
                Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
                if (subscription != null && subscription.getStatus() != null) {
                    String status = subscription.getStatus();
                    if (!"canceled".equalsIgnoreCase(status) && !"incomplete_expired".equalsIgnoreCase(status)) {
                        subscription.cancel();
                    }
                }
            }

            if (!isBlank(stripeCustomerId)) {
                Customer customer = Customer.retrieve(stripeCustomerId);
                if (customer != null && !Boolean.TRUE.equals(customer.getDeleted())) {
                    customer.delete();
                }
            }
        } catch (StripeException ex) {
            throw new StripeBillingException("Unable to cancel Stripe billing for account deletion", ex);
        }
    }

    @Override
    public BillingWebhookEvent parseWebhookEvent(String payload, String signatureHeader) {
        requireWebhookConfigured();
        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, properties.getWebhookSecret());
            StripeObject stripeObject = deserializeEventObject(event.getDataObjectDeserializer());
            if (stripeObject == null) {
                return BillingWebhookEvent.builder().type(BillingWebhookEventType.IGNORED).build();
            }

            return switch (event.getType()) {
                case "checkout.session.completed" -> fromCheckoutSession((Session) stripeObject);
                case "customer.subscription.created" -> fromSubscription(BillingWebhookEventType.SUBSCRIPTION_CREATED, (Subscription) stripeObject);
                case "customer.subscription.updated" -> fromSubscription(BillingWebhookEventType.SUBSCRIPTION_UPDATED, (Subscription) stripeObject);
                case "customer.subscription.deleted" -> fromSubscription(BillingWebhookEventType.SUBSCRIPTION_DELETED, (Subscription) stripeObject);
                default -> BillingWebhookEvent.builder().type(BillingWebhookEventType.IGNORED).build();
            };
        } catch (SignatureVerificationException ex) {
            throw new StripeWebhookException("Invalid Stripe webhook signature", ex);
        } catch (RuntimeException ex) {
            throw new StripeWebhookException("Unable to parse Stripe webhook payload", ex);
        }
    }

    StripeObject deserializeEventObject(EventDataObjectDeserializer deserializer) {
        if (deserializer == null) {
            return null;
        }
        return deserializer.getObject().orElseGet(() -> {
            try {
                return deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException ex) {
                throw new StripeWebhookException("Unable to deserialize Stripe webhook event", ex);
            }
        });
    }

    private BillingWebhookEvent fromCheckoutSession(Session session) {
        String userId = session.getClientReferenceId();
        if ((userId == null || userId.isBlank()) && session.getMetadata() != null) {
            userId = session.getMetadata().get("userId");
        }
        return BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.CHECKOUT_COMPLETED)
                .userId(userId)
                .stripeCustomerId(session.getCustomer())
                .stripeSubscriptionId(session.getSubscription())
                .build();
    }

    private BillingWebhookEvent fromSubscription(BillingWebhookEventType type, Subscription subscription) {
        Map<String, String> metadata = subscription.getMetadata();
        String priceId = subscription.getItems() != null
                && subscription.getItems().getData() != null
                && !subscription.getItems().getData().isEmpty()
                && subscription.getItems().getData().get(0).getPrice() != null
                ? subscription.getItems().getData().get(0).getPrice().getId()
                : null;

        return BillingWebhookEvent.builder()
                .type(type)
                .userId(metadata == null ? null : metadata.get("userId"))
                .stripeCustomerId(subscription.getCustomer())
                .stripeSubscriptionId(subscription.getId())
                .stripePriceId(priceId)
                .stripeSubscriptionStatus(subscription.getStatus())
                .build();
    }

    private void requireCheckoutConfigured(BillingCheckoutSessionRequest request) {
        if (!properties.isEnabled()
                || isBlank(properties.getSecretKey())
                || request == null
                || isBlank(request.getPriceId())) {
            throw new BillingNotConfiguredException();
        }
    }

    private void requireWebhookConfigured() {
        if (!properties.isEnabled()
                || isBlank(properties.getWebhookSecret())) {
            throw new BillingNotConfiguredException();
        }
    }

    private void requirePortalConfigured(String stripeCustomerId) {
        if (!properties.isEnabled()
                || isBlank(properties.getSecretKey())
                || isBlank(properties.getPortalReturnUrl())
                || isBlank(stripeCustomerId)) {
            throw new BillingNotConfiguredException();
        }
    }

    private void requireSubscriptionMutationConfigured(String stripeSubscriptionId, String value) {
        if (!properties.isEnabled()
                || isBlank(properties.getSecretKey())
                || isBlank(stripeSubscriptionId)
                || isBlank(value)) {
            throw new BillingNotConfiguredException();
        }
    }

    private String resolvePrimarySubscriptionItemId(Subscription subscription) {
        if (subscription == null
                || subscription.getItems() == null
                || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()
                || subscription.getItems().getData().get(0) == null
                || isBlank(subscription.getItems().getData().get(0).getId())) {
            throw new BillingStateException("Stripe subscription has no mutable subscription item");
        }
        return subscription.getItems().getData().get(0).getId();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
