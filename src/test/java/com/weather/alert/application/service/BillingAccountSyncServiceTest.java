package com.weather.alert.application.service;

import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.BillingWebhookEventType;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingAccountSyncServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private AlertCriteriaRepositoryPort alertCriteriaRepositoryPort;

    @Mock
    private BillingPlanService billingPlanService;

    @InjectMocks
    private BillingAccountSyncService billingAccountSyncService;

    @Test
    void shouldApplyIdentifiersAndPersistUpdatedUser() {
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .build();
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.CHECKOUT_COMPLETED)
                .stripeCustomerId("cus_123")
                .stripeSubscriptionId("sub_123")
                .stripePriceId("price_plus")
                .stripeSubscriptionStatus("active")
                .build();

        when(billingPlanService.resolveEntitlements(any(User.class))).thenReturn(BillingEntitlements.builder()
                .plan(BillingPlan.FREE)
                .paidPlan(false)
                .maxActiveAlerts(1)
                .adSponsoredEmails(true)
                .build());
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = billingAccountSyncService.applyBillingUpdate(user, event);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(userCaptor.capture());
        assertEquals("cus_123", userCaptor.getValue().getStripeCustomerId());
        assertEquals("sub_123", userCaptor.getValue().getStripeSubscriptionId());
        assertEquals("price_plus", userCaptor.getValue().getStripePriceId());
        assertEquals("active", userCaptor.getValue().getStripeSubscriptionStatus());
        assertEquals("sub_123", savedUser.getStripeSubscriptionId());
    }

    @Test
    void shouldMarkDeletedSubscriptionCanceledWhenStatusMissing() {
        User user = User.builder()
                .id("user-1")
                .stripeCustomerId("cus_123")
                .stripeSubscriptionStatus("active")
                .stripePriceId("price_pro")
                .build();
        BillingWebhookEvent event = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.SUBSCRIPTION_DELETED)
                .stripeSubscriptionId("sub_123")
                .build();

        when(billingPlanService.resolveEntitlements(any(User.class))).thenReturn(BillingEntitlements.builder()
                .plan(BillingPlan.FREE)
                .paidPlan(false)
                .maxActiveAlerts(1)
                .adSponsoredEmails(true)
                .build());
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = billingAccountSyncService.applyBillingUpdate(user, event);

        assertEquals("sub_123", savedUser.getStripeSubscriptionId());
        assertEquals("canceled", savedUser.getStripeSubscriptionStatus());
        assertNull(savedUser.getStripePriceId());
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
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertCriteriaRepositoryPort.findByUserId("user-1")).thenReturn(List.of(oldest, second, third));

        billingAccountSyncService.applyBillingUpdate(user, event);

        verify(userRepositoryPort).save(argThat(savedUser -> "canceled".equals(savedUser.getStripeSubscriptionStatus())));
        verify(alertCriteriaRepositoryPort, times(2)).save(argThat(criteria -> Boolean.FALSE.equals(criteria.getEnabled())));
        verify(alertCriteriaRepositoryPort, never()).save(argThat(criteria -> "criteria-1".equals(criteria.getId())));
        assertTrue(oldest.getEnabled());
        assertFalse(second.getEnabled());
        assertFalse(third.getEnabled());
    }
}
