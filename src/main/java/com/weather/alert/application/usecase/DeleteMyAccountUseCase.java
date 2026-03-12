package com.weather.alert.application.usecase;

import com.weather.alert.application.exception.StripeBillingException;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertCriteriaRepository;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertCriteriaStateRepository;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeleteMyAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteMyAccountUseCase.class);

    private final UserRepositoryPort userRepository;
    private final BillingProviderPort billingProviderPort;
    private final JpaAlertRepository jpaAlertRepository;
    private final JpaAlertCriteriaRepository jpaAlertCriteriaRepository;
    private final JpaAlertCriteriaStateRepository jpaAlertCriteriaStateRepository;

    @Transactional
    public void delete(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        cancelExternalBillingIfPresent(user);

        List<String> criteriaIds = jpaAlertCriteriaRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .map(criteria -> criteria.getId())
                .toList();

        jpaAlertRepository.deleteByUserId(userId);
        if (!criteriaIds.isEmpty()) {
            jpaAlertCriteriaStateRepository.deleteByCriteriaIds(criteriaIds);
        }
        jpaAlertCriteriaRepository.deleteByUserId(userId);
        userRepository.delete(userId);

        log.info("ACCOUNT_DELETED userId={}", userId);
    }

    private void cancelExternalBillingIfPresent(User user) {
        if ((user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank())
                && (user.getStripeSubscriptionId() == null || user.getStripeSubscriptionId().isBlank())) {
            return;
        }

        try {
            billingProviderPort.cancelCustomerBilling(user.getStripeCustomerId(), user.getStripeSubscriptionId());
        } catch (StripeBillingException ex) {
            log.warn("ACCOUNT_DELETE_BILLING_CLEANUP_FAILED userId={} message={}", user.getId(), ex.getMessage());
            throw ex;
        }
    }
}
