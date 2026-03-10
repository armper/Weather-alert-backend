package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.BillingCheckoutSessionRequest;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateBillingCheckoutSessionUseCase {

    private static final Set<String> ACTIVE_STATUSES = Set.of("trialing", "active", "past_due");

    private final UserRepositoryPort userRepositoryPort;
    private final BillingProviderPort billingProviderPort;
    private final BillingPlanService billingPlanService;

    public BillingCheckoutSessionResponse createForUser(String userId) {
        return createForUser(userId, BillingPlan.PLUS);
    }

    public BillingCheckoutSessionResponse createForUser(String userId, BillingPlan requestedPlan) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getStripeSubscriptionStatus() != null
                && ACTIVE_STATUSES.contains(user.getStripeSubscriptionStatus())) {
            throw new BillingStateException("User already has an active Stripe subscription");
        }

        BillingCheckoutSession session = billingProviderPort.createSubscriptionCheckoutSession(
                BillingCheckoutSessionRequest.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .stripeCustomerId(user.getStripeCustomerId())
                        .plan(requestedPlan == null ? BillingPlan.PLUS : requestedPlan)
                        .priceId(billingPlanService.resolveCheckoutPriceId(requestedPlan))
                        .build());

        return BillingCheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .url(session.getUrl())
                .build();
    }
}
