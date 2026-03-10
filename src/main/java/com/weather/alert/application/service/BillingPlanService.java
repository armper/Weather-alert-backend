package com.weather.alert.application.service;

import com.weather.alert.application.exception.BillingNotConfiguredException;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.User;
import com.weather.alert.infrastructure.config.StripeBillingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class BillingPlanService {

    private static final Set<String> ACTIVE_SUBSCRIPTION_STATUSES = Set.of("trialing", "active", "past_due");

    private final StripeBillingProperties stripeBillingProperties;

    public BillingPlan resolvePlan(User user) {
        if (!hasActiveSubscription(user)) {
            return BillingPlan.FREE;
        }
        return resolvePlanByPriceId(user == null ? null : user.getStripePriceId());
    }

    public BillingEntitlements resolveEntitlements(User user) {
        return resolveEntitlements(resolvePlan(user));
    }

    public BillingEntitlements resolveEntitlements(BillingPlan plan) {
        return switch (plan == null ? BillingPlan.FREE : plan) {
            case FREE -> BillingEntitlements.builder()
                    .plan(BillingPlan.FREE)
                    .paidPlan(false)
                    .maxActiveAlerts(1)
                    .adSponsoredEmails(true)
                    .build();
            case PLUS -> BillingEntitlements.builder()
                    .plan(BillingPlan.PLUS)
                    .paidPlan(true)
                    .maxActiveAlerts(10)
                    .adSponsoredEmails(false)
                    .build();
            case PRO -> BillingEntitlements.builder()
                    .plan(BillingPlan.PRO)
                    .paidPlan(true)
                    .maxActiveAlerts(50)
                    .adSponsoredEmails(false)
                    .build();
        };
    }

    public String resolveCheckoutPriceId(BillingPlan requestedPlan) {
        BillingPlan plan = requestedPlan == null ? BillingPlan.PLUS : requestedPlan;
        if (plan == BillingPlan.FREE) {
            throw new BillingStateException("The free plan does not use Stripe Checkout");
        }

        String priceId = switch (plan) {
            case PLUS -> firstNonBlank(stripeBillingProperties.getPlusPriceId(), stripeBillingProperties.getPriceId());
            case PRO -> stripeBillingProperties.getProPriceId();
            case FREE -> null;
        };

        if (priceId == null || priceId.isBlank()) {
            throw new BillingNotConfiguredException();
        }
        return priceId;
    }

    public boolean hasActiveSubscription(User user) {
        if (user == null || user.getStripeSubscriptionStatus() == null) {
            return false;
        }
        return ACTIVE_SUBSCRIPTION_STATUSES.contains(user.getStripeSubscriptionStatus());
    }

    private BillingPlan resolvePlanByPriceId(String priceId) {
        if (matches(priceId, stripeBillingProperties.getProPriceId())) {
            return BillingPlan.PRO;
        }
        if (matches(priceId, stripeBillingProperties.getPlusPriceId())
                || matches(priceId, stripeBillingProperties.getPriceId())) {
            return BillingPlan.PLUS;
        }
        return BillingPlan.FREE;
    }

    private boolean matches(String left, String right) {
        return left != null && right != null && !left.isBlank() && left.equals(right);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }
}
