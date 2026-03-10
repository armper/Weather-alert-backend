package com.weather.alert.infrastructure.adapter.stripe;

import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
