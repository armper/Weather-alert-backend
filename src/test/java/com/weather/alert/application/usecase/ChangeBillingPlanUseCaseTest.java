package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.application.service.BillingAccountSyncService;
import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.BillingWebhookEventType;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeBillingPlanUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private BillingProviderPort billingProviderPort;

    @Mock
    private BillingPlanService billingPlanService;

    @Mock
    private BillingAccountSyncService billingAccountSyncService;

    @Mock
    private GetBillingStatusUseCase getBillingStatusUseCase;

    @InjectMocks
    private ChangeBillingPlanUseCase useCase;

    @Test
    void shouldUpgradeExistingPaidSubscription() {
        User user = User.builder()
                .id("user-1")
                .stripeSubscriptionId("sub_123")
                .stripeSubscriptionStatus("active")
                .stripePriceId("price_plus")
                .build();
        BillingWebhookEvent updateEvent = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.SUBSCRIPTION_UPDATED)
                .stripeSubscriptionId("sub_123")
                .stripePriceId("price_pro")
                .stripeSubscriptionStatus("active")
                .build();

        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));
        when(billingPlanService.resolvePlan(user)).thenReturn(BillingPlan.PLUS);
        when(billingPlanService.hasActiveSubscription(user)).thenReturn(true);
        when(billingPlanService.resolveCheckoutPriceId(BillingPlan.PRO)).thenReturn("price_pro");
        when(billingProviderPort.changeSubscriptionPlan("sub_123", "price_pro")).thenReturn(updateEvent);
        when(getBillingStatusUseCase.getForUser("user-1")).thenReturn(BillingStatusResponse.builder()
                .userId("user-1")
                .plan(BillingPlan.PRO)
                .activeSubscription(true)
                .build());

        BillingStatusResponse response = useCase.changeForUser("user-1", BillingPlan.PRO);

        verify(billingProviderPort).changeSubscriptionPlan("sub_123", "price_pro");
        verify(billingAccountSyncService).applyBillingUpdate(user, updateEvent);
        assertEquals(BillingPlan.PRO, response.getPlan());
    }

    @Test
    void shouldDowngradePaidSubscriptionToFree() {
        User user = User.builder()
                .id("user-1")
                .stripeSubscriptionId("sub_123")
                .stripeSubscriptionStatus("active")
                .stripePriceId("price_plus")
                .build();
        BillingWebhookEvent updateEvent = BillingWebhookEvent.builder()
                .type(BillingWebhookEventType.SUBSCRIPTION_DELETED)
                .stripeSubscriptionId("sub_123")
                .stripeSubscriptionStatus("canceled")
                .build();

        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));
        when(billingPlanService.resolvePlan(user)).thenReturn(BillingPlan.PLUS);
        when(billingPlanService.hasActiveSubscription(user)).thenReturn(true);
        when(billingProviderPort.cancelSubscription("sub_123")).thenReturn(updateEvent);
        when(getBillingStatusUseCase.getForUser("user-1")).thenReturn(BillingStatusResponse.builder()
                .userId("user-1")
                .plan(BillingPlan.FREE)
                .activeSubscription(false)
                .build());

        BillingStatusResponse response = useCase.changeForUser("user-1", BillingPlan.FREE);

        verify(billingProviderPort).cancelSubscription("sub_123");
        verify(billingAccountSyncService).applyBillingUpdate(user, updateEvent);
        assertEquals(BillingPlan.FREE, response.getPlan());
    }

    @Test
    void shouldRejectChangingToSamePlan() {
        User user = User.builder()
                .id("user-1")
                .stripeSubscriptionStatus("active")
                .build();

        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));
        when(billingPlanService.resolvePlan(user)).thenReturn(BillingPlan.PLUS);

        assertThrows(BillingStateException.class, () -> useCase.changeForUser("user-1", BillingPlan.PLUS));
    }
}
