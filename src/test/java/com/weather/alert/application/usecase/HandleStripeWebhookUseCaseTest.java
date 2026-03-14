package com.weather.alert.application.usecase;

import com.weather.alert.application.service.BillingAccountSyncService;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.BillingWebhookEventType;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleStripeWebhookUseCaseTest {

    @Mock
    private BillingProviderPort billingProviderPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private BillingAccountSyncService billingAccountSyncService;

    @InjectMocks
    private HandleStripeWebhookUseCase useCase;

    @Test
    void shouldUpdateUserResolvedByUserId() {
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .build();
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.CHECKOUT_COMPLETED)
                .userId("user-1")
                .stripeCustomerId("cus_123")
                .stripeSubscriptionId("sub_123")
                .build();

        when(billingProviderPort.parseWebhookEvent("payload", "signature")).thenReturn(event);
        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));

        useCase.handle("payload", "signature");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<BillingWebhookEvent> eventCaptor = ArgumentCaptor.forClass(BillingWebhookEvent.class);
        verify(billingAccountSyncService).applyBillingUpdate(userCaptor.capture(), eventCaptor.capture());
        assertEquals("user-1", userCaptor.getValue().getId());
        assertEquals("cus_123", eventCaptor.getValue().getStripeCustomerId());
        assertEquals("sub_123", eventCaptor.getValue().getStripeSubscriptionId());
    }

    @Test
    void shouldFallbackToCustomerLookupAndMarkDeletedSubscriptionCanceled() {
        User user = User.builder()
                .id("user-1")
                .stripeCustomerId("cus_123")
                .stripeSubscriptionStatus("active")
                .build();
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.SUBSCRIPTION_DELETED)
                .stripeCustomerId("cus_123")
                .stripeSubscriptionId("sub_123")
                .build();

        when(billingProviderPort.parseWebhookEvent("payload", "signature")).thenReturn(event);
        when(userRepositoryPort.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.empty());
        when(userRepositoryPort.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        useCase.handle("payload", "signature");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<BillingWebhookEvent> eventCaptor = ArgumentCaptor.forClass(BillingWebhookEvent.class);
        verify(billingAccountSyncService).applyBillingUpdate(userCaptor.capture(), eventCaptor.capture());
        assertEquals("user-1", userCaptor.getValue().getId());
        assertEquals(BillingWebhookEventType.SUBSCRIPTION_DELETED, eventCaptor.getValue().getType());
        assertEquals("sub_123", eventCaptor.getValue().getStripeSubscriptionId());
    }

    @Test
    void shouldIgnoreWebhookEventsWithoutResolvedUser() {
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.SUBSCRIPTION_UPDATED)
                .stripeCustomerId("cus_missing")
                .build();

        when(billingProviderPort.parseWebhookEvent("payload", "signature")).thenReturn(event);
        when(userRepositoryPort.findByStripeCustomerId("cus_missing")).thenReturn(Optional.empty());

        useCase.handle("payload", "signature");

        verify(billingAccountSyncService, never()).applyBillingUpdate(any(), any());
    }

    @Test
    void shouldIgnoreExplicitlyIgnoredEvents() {
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.IGNORED)
                .build();

        when(billingProviderPort.parseWebhookEvent("payload", "signature")).thenReturn(event);

        useCase.handle("payload", "signature");

        verify(userRepositoryPort, never()).findById(org.mockito.ArgumentMatchers.anyString());
        verify(billingAccountSyncService, never()).applyBillingUpdate(any(), any());
    }
}
