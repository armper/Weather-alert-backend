package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.application.service.BillingAccountSyncService;
import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeBillingPlanUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final BillingProviderPort billingProviderPort;
    private final BillingPlanService billingPlanService;
    private final BillingAccountSyncService billingAccountSyncService;
    private final GetBillingStatusUseCase getBillingStatusUseCase;

    @Transactional
    public BillingStatusResponse changeForUser(String userId, BillingPlan requestedPlan) {
        BillingPlan targetPlan = requestedPlan == null ? BillingPlan.PLUS : requestedPlan;

        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        BillingPlan currentPlan = billingPlanService.resolvePlan(user);
        if (targetPlan == currentPlan) {
            throw new BillingStateException("User is already on the requested plan");
        }

        BillingWebhookEvent updateEvent;
        if (targetPlan == BillingPlan.FREE) {
            if (!billingPlanService.hasActiveSubscription(user) || isBlank(user.getStripeSubscriptionId())) {
                throw new BillingStateException("User does not have an active paid subscription to downgrade");
            }
            updateEvent = billingProviderPort.cancelSubscription(user.getStripeSubscriptionId());
        } else if (billingPlanService.hasActiveSubscription(user)) {
            if (isBlank(user.getStripeSubscriptionId())) {
                throw new BillingStateException("Active subscription is missing Stripe subscription details");
            }
            updateEvent = billingProviderPort.changeSubscriptionPlan(
                    user.getStripeSubscriptionId(),
                    billingPlanService.resolveCheckoutPriceId(targetPlan));
        } else {
            throw new BillingStateException("Use Stripe Checkout to start a new paid subscription");
        }

        billingAccountSyncService.applyBillingUpdate(user, updateEvent);
        return getBillingStatusUseCase.getForUser(userId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
