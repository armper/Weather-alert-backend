package com.weather.alert.application.usecase;

import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.BillingWebhookEvent;
import com.weather.alert.domain.model.BillingWebhookEventType;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HandleStripeWebhookUseCase {

    private final BillingProviderPort billingProviderPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AlertCriteriaRepositoryPort alertCriteriaRepositoryPort;
    private final BillingPlanService billingPlanService;

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
        BillingEntitlements previousEntitlements = billingPlanService.resolveEntitlements(user);
        User updatedUser = applyEvent(user, event);
        BillingEntitlements updatedEntitlements = billingPlanService.resolveEntitlements(updatedUser);

        userRepositoryPort.save(updatedUser);
        disableExcessActiveCriteria(updatedUser.getId(), previousEntitlements, updatedEntitlements);
    }

    private void disableExcessActiveCriteria(
            String userId,
            BillingEntitlements previousEntitlements,
            BillingEntitlements updatedEntitlements) {
        if (userId == null || userId.isBlank() || previousEntitlements == null || updatedEntitlements == null) {
            return;
        }
        if (updatedEntitlements.getMaxActiveAlerts() >= previousEntitlements.getMaxActiveAlerts()) {
            return;
        }

        List<AlertCriteria> enabledCriteria = alertCriteriaRepositoryPort.findByUserId(userId).stream()
                .filter(criteria -> Boolean.TRUE.equals(criteria.getEnabled()))
                .toList();

        if (enabledCriteria.size() <= updatedEntitlements.getMaxActiveAlerts()) {
            return;
        }

        enabledCriteria.stream()
                .skip(updatedEntitlements.getMaxActiveAlerts())
                .forEach(criteria -> {
                    criteria.setEnabled(false);
                    alertCriteriaRepositoryPort.save(criteria);
                });
    }

    private User applyEvent(User user, BillingWebhookEvent event) {
        if (event.getStripeCustomerId() != null && !event.getStripeCustomerId().isBlank()) {
            user.setStripeCustomerId(event.getStripeCustomerId());
        }
        if (event.getStripeSubscriptionId() != null && !event.getStripeSubscriptionId().isBlank()) {
            user.setStripeSubscriptionId(event.getStripeSubscriptionId());
        }
        if (event.getStripePriceId() != null && !event.getStripePriceId().isBlank()) {
            user.setStripePriceId(event.getStripePriceId());
        }
        if (event.getStripeSubscriptionStatus() != null && !event.getStripeSubscriptionStatus().isBlank()) {
            user.setStripeSubscriptionStatus(event.getStripeSubscriptionStatus());
        }
        if (event.getStripeCurrentPeriodEnd() != null) {
            user.setStripeCurrentPeriodEnd(event.getStripeCurrentPeriodEnd());
        }
        if (event.getType() == BillingWebhookEventType.SUBSCRIPTION_DELETED
                && (event.getStripeSubscriptionStatus() == null || event.getStripeSubscriptionStatus().isBlank())) {
            user.setStripeSubscriptionStatus("canceled");
        }
        return user;
    }
}
