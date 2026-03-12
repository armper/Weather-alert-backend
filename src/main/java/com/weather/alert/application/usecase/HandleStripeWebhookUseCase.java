package com.weather.alert.application.usecase;

import com.weather.alert.application.service.BillingAccountSyncService;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.BillingWebhookEventType;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HandleStripeWebhookUseCase {

    private final BillingProviderPort billingProviderPort;
    private final UserRepositoryPort userRepositoryPort;
    private final BillingAccountSyncService billingAccountSyncService;

    @Transactional
    public void handle(String payload, String signatureHeader) {
        BillingWebhookEvent event = billingProviderPort.parseWebhookEvent(payload, signatureHeader);
        if (event.getType() == BillingWebhookEventType.IGNORED) {
            return;
        }

        resolveUser(event).ifPresent(user -> persistUpdatedBillingState(user, event));
    }

    private Optional<User> resolveUser(BillingWebhookEvent event) {
        if (event.getUserId() != null && !event.getUserId().isBlank()) {
            Optional<User> user = userRepositoryPort.findById(event.getUserId());
            if (user.isPresent()) {
                return user;
            }
        }
        if (event.getStripeSubscriptionId() != null && !event.getStripeSubscriptionId().isBlank()) {
            Optional<User> user = userRepositoryPort.findByStripeSubscriptionId(event.getStripeSubscriptionId());
            if (user.isPresent()) {
                return user;
            }
        }
        if (event.getStripeCustomerId() != null && !event.getStripeCustomerId().isBlank()) {
            return userRepositoryPort.findByStripeCustomerId(event.getStripeCustomerId());
        }
        return Optional.empty();
    }

    private void persistUpdatedBillingState(User user, BillingWebhookEvent event) {
        billingAccountSyncService.applyBillingUpdate(user, event);
    }
}
