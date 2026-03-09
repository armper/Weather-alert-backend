package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.BillingCheckoutSessionRequest;
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

    public BillingCheckoutSessionResponse createForUser(String userId) {
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
                        .build());

        return BillingCheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .url(session.getUrl())
                .build();
    }
}
