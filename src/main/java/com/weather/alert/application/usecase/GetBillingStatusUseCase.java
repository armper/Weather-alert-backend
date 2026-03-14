package com.weather.alert.application.usecase;

import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetBillingStatusUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final BillingPlanService billingPlanService;

    @Value("${app.security.user.username:}")
    private String bootstrapUserUsername;

    @Value("${app.security.admin.username:}")
    private String bootstrapAdminUsername;

    public BillingStatusResponse getForUser(String userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseGet(() -> reservedBillingUser(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId)));

        return toResponse(user);
    }

    public BillingStatusResponse toResponse(User user) {
        BillingEntitlements entitlements = billingPlanService.resolveEntitlements(user);
        return BillingStatusResponse.builder()
                .userId(user.getId())
                .plan(entitlements.getPlan())
                .paidPlan(entitlements.isPaidPlan())
                .maxActiveAlerts(entitlements.getMaxActiveAlerts())
                .maxTravelPlans(entitlements.getMaxTravelPlans())
                .adSponsoredEmails(entitlements.isAdSponsoredEmails())
                .stripeCustomerId(user.getStripeCustomerId())
                .stripeSubscriptionId(user.getStripeSubscriptionId())
                .stripePriceId(user.getStripePriceId())
                .stripeSubscriptionStatus(user.getStripeSubscriptionStatus())
                .stripeCurrentPeriodEnd(user.getStripeCurrentPeriodEnd())
                .activeSubscription(billingPlanService.hasActiveSubscription(user))
                .build();
    }

    private Optional<User> reservedBillingUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        if (matchesBootstrapUser(userId, bootstrapAdminUsername)) {
            return Optional.of(User.builder()
                    .id(userId)
                    .email(userId)
                    .name("SkyPanda Admin")
                    .role("ROLE_ADMIN")
                    .build());
        }
        if (matchesBootstrapUser(userId, bootstrapUserUsername)) {
            return Optional.of(User.builder()
                    .id(userId)
                    .email(userId)
                    .name("SkyPanda User")
                    .role("ROLE_USER")
                    .build());
        }
        return Optional.empty();
    }

    private boolean matchesBootstrapUser(String userId, String configuredUsername) {
        return userId.toLowerCase(Locale.US).equals(configuredUsername == null ? "" : configuredUsername.trim().toLowerCase(Locale.US));
    }
}
