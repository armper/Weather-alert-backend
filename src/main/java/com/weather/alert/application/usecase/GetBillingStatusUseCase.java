package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetBillingStatusUseCase {

    private static final Set<String> ACTIVE_STATUSES = Set.of("trialing", "active", "past_due");

    private final UserRepositoryPort userRepositoryPort;

    public BillingStatusResponse getForUser(String userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String status = user.getStripeSubscriptionStatus();
        return BillingStatusResponse.builder()
                .userId(user.getId())
                .stripeCustomerId(user.getStripeCustomerId())
                .stripeSubscriptionId(user.getStripeSubscriptionId())
                .stripePriceId(user.getStripePriceId())
                .stripeSubscriptionStatus(status)
                .stripeCurrentPeriodEnd(user.getStripeCurrentPeriodEnd())
                .activeSubscription(status != null && ACTIVE_STATUSES.contains(status))
                .build();
    }
}
