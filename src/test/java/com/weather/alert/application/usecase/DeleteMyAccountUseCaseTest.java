package com.weather.alert.application.usecase;

import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.infrastructure.adapter.persistence.AlertCriteriaEntity;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertCriteriaRepository;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertCriteriaStateRepository;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertRepository;
import com.weather.alert.infrastructure.adapter.persistence.JpaTravelPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteMyAccountUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private BillingProviderPort billingProviderPort;

    @Mock
    private JpaAlertRepository jpaAlertRepository;

    @Mock
    private JpaAlertCriteriaRepository jpaAlertCriteriaRepository;

    @Mock
    private JpaAlertCriteriaStateRepository jpaAlertCriteriaStateRepository;

    @Mock
    private JpaTravelPlanRepository jpaTravelPlanRepository;

    private DeleteMyAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteMyAccountUseCase(
                userRepository,
                billingProviderPort,
                jpaAlertRepository,
                jpaAlertCriteriaRepository,
                jpaAlertCriteriaStateRepository,
                jpaTravelPlanRepository);
    }

    @Test
    void shouldDeleteUserAndAssociatedData() {
        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .stripeCustomerId("cus_123")
                .stripeSubscriptionId("sub_123")
                .build()));
        when(jpaAlertCriteriaRepository.findByUserIdOrderByCreatedAtAscIdAsc("alice")).thenReturn(List.of(
                AlertCriteriaEntity.builder().id("criteria-1").createdAt(Instant.now()).build(),
                AlertCriteriaEntity.builder().id("criteria-2").createdAt(Instant.now()).build()));

        useCase.delete("alice");

        verify(billingProviderPort).cancelCustomerBilling("cus_123", "sub_123");
        verify(jpaAlertRepository).deleteByUserId("alice");
        verify(jpaAlertCriteriaStateRepository).deleteByCriteriaIds(List.of("criteria-1", "criteria-2"));
        verify(jpaAlertCriteriaRepository).deleteByUserId("alice");
        verify(jpaTravelPlanRepository).deleteByUserId("alice");
        verify(userRepository).delete("alice");
    }

    @Test
    void shouldSkipCriteriaStateDeleteWhenUserHasNoCriteria() {
        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .build()));
        when(jpaAlertCriteriaRepository.findByUserIdOrderByCreatedAtAscIdAsc("alice")).thenReturn(List.of());

        useCase.delete("alice");

        verify(billingProviderPort, never()).cancelCustomerBilling(null, null);
        verify(jpaAlertRepository).deleteByUserId("alice");
        verify(jpaAlertCriteriaStateRepository, never()).deleteByCriteriaIds(List.of());
        verify(jpaAlertCriteriaRepository).deleteByUserId("alice");
        verify(jpaTravelPlanRepository).deleteByUserId("alice");
        verify(userRepository).delete("alice");
    }
}
