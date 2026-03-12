package com.weather.alert.application.usecase;

import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBillingStatusUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final BillingPlanService billingPlanService;

    public BillingStatusResponse getForUser(String userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return toResponse(user);
    }

    public BillingStatusResponse toResponse(User user) {
        BillingEntitlements entitlements = billingPlanService.resolveEntitlements(user);
        return BillingStatusResponse.builder()
                .userId(user.getId())
                .plan(entitlements.getPlan())
                .paidPlan(entitlements.isPaidPlan())
                .maxActiveAlerts(entitlements.getMaxActiveAlerts())
                .adSponsoredEmails(entitlements.isAdSponsoredEmails())
                .stripeCustomerId(user.getStripeCustomerId())
                .stripeSubscriptionId(user.getStripeSubscriptionId())
                .stripePriceId(user.getStripePriceId())
                .stripeSubscriptionStatus(user.getStripeSubscriptionStatus())
                .stripeCurrentPeriodEnd(user.getStripeCurrentPeriodEnd())
                .activeSubscription(billingPlanService.hasActiveSubscription(user))
                .build();
    }
}
