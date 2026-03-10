package com.weather.alert.infrastructure.adapter.stripe;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.weather.alert.application.exception.StripeWebhookException;
import com.weather.alert.infrastructure.config.StripeBillingProperties;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StripeBillingAdapterTest {

    private final StripeBillingAdapter adapter = new StripeBillingAdapter(new StripeBillingProperties());

    @Test
    void shouldFallbackToUnsafeDeserializationWhenSafeObjectIsUnavailable() throws Exception {
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        Subscription subscription = new Subscription();

        when(deserializer.getObject()).thenReturn(Optional.empty());
        when(deserializer.deserializeUnsafe()).thenReturn(subscription);

        StripeObject result = adapter.deserializeEventObject(deserializer);

        assertSame(subscription, result);
    }

    @Test
    void shouldWrapUnsafeDeserializationFailure() throws Exception {
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(deserializer.getObject()).thenReturn(Optional.empty());
        when(deserializer.deserializeUnsafe()).thenThrow(new EventDataObjectDeserializationException(
                "bad payload", "customer.subscription.created"));

        assertThrows(StripeWebhookException.class, () -> adapter.deserializeEventObject(deserializer));
    }
}
