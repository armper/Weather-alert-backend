package com.weather.alert.application.usecase;

import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.BillingWebhookEventType;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class HandleStripeWebhookUseCaseTest {

    @Mock
    private BillingProviderPort billingProviderPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private AlertCriteriaRepositoryPort alertCriteriaRepositoryPort;

    @Mock
    private BillingPlanService billingPlanService;

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
        verify(userRepositoryPort).save(userCaptor.capture());
        assertEquals("cus_123", userCaptor.getValue().getStripeCustomerId());
        assertEquals("sub_123", userCaptor.getValue().getStripeSubscriptionId());
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
        verify(userRepositoryPort).save(userCaptor.capture());
        assertEquals("sub_123", userCaptor.getValue().getStripeSubscriptionId());
        assertEquals("canceled", userCaptor.getValue().getStripeSubscriptionStatus());
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

        verify(userRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldIgnoreExplicitlyIgnoredEvents() {
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.IGNORED)
                .build();

        when(billingProviderPort.parseWebhookEvent("payload", "signature")).thenReturn(event);

        useCase.handle("payload", "signature");

        verify(userRepositoryPort, never()).findById(org.mockito.ArgumentMatchers.anyString());
        verify(userRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldDisableExcessCriteriaWhenDowngradedToFreePlan() {
        User user = User.builder()
                .id("user-1")
                .stripeSubscriptionStatus("active")
                .stripePriceId("price_pro")
                .build();
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.SUBSCRIPTION_DELETED)
                .userId("user-1")
                .build();
        AlertCriteria oldest = AlertCriteria.builder()
                .id("criteria-1")
                .userId("user-1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-01T00:00:00Z"))
                .build();
        AlertCriteria second = AlertCriteria.builder()
                .id("criteria-2")
                .userId("user-1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-02T00:00:00Z"))
                .build();
        AlertCriteria third = AlertCriteria.builder()
                .id("criteria-3")
                .userId("user-1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-03T00:00:00Z"))
                .build();

        when(billingProviderPort.parseWebhookEvent("payload", "signature")).thenReturn(event);
        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));
        when(billingPlanService.resolveEntitlements(any(User.class)))
                .thenAnswer(invocation -> {
                    User candidate = invocation.getArgument(0);
                    if ("active".equals(candidate.getStripeSubscriptionStatus())) {
                        return BillingEntitlements.builder()
                                .plan(BillingPlan.PRO)
                                .paidPlan(true)
                                .maxActiveAlerts(50)
                                .adSponsoredEmails(false)
                                .build();
                    }
                    return BillingEntitlements.builder()
                            .plan(BillingPlan.FREE)
                            .paidPlan(false)
                            .maxActiveAlerts(1)
                            .adSponsoredEmails(true)
                            .build();
                });
        when(alertCriteriaRepositoryPort.findByUserId("user-1")).thenReturn(List.of(oldest, second, third));

        useCase.handle("payload", "signature");

        verify(userRepositoryPort).save(argThat(savedUser -> "canceled".equals(savedUser.getStripeSubscriptionStatus())));
        verify(alertCriteriaRepositoryPort, times(2)).save(argThat(criteria -> Boolean.FALSE.equals(criteria.getEnabled())));
        verify(alertCriteriaRepositoryPort, never()).save(argThat(criteria -> "criteria-1".equals(criteria.getId())));
        assertTrue(oldest.getEnabled());
        assertFalse(second.getEnabled());
        assertFalse(third.getEnabled());
    }
}
