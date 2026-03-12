package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateBillingPortalSessionUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final BillingProviderPort billingProviderPort;

    public BillingCheckoutSessionResponse createForUser(String userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
            throw new BillingStateException("User does not have a Stripe customer record");
        }

        BillingCheckoutSession session = billingProviderPort.createCustomerPortalSession(user.getStripeCustomerId());
        return BillingCheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .url(session.getUrl())
                .build();
    }
}
