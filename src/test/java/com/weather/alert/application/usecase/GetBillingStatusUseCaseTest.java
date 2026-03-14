package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBillingStatusUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private BillingPlanService billingPlanService;

    @InjectMocks
    private GetBillingStatusUseCase useCase;

    @Test
    void shouldReturnSyntheticBillingStatusForReservedAdminUser() {
        ReflectionTestUtils.setField(useCase, "bootstrapAdminUsername", "weather-admin");
        when(userRepositoryPort.findById("weather-admin")).thenReturn(Optional.empty());
        when(billingPlanService.resolveEntitlements(argThat((User user) -> user != null && "weather-admin".equals(user.getId()))))
                .thenReturn(BillingEntitlements.builder()
                        .plan(BillingPlan.FREE)
                        .paidPlan(false)
                        .maxActiveAlerts(1)
                        .maxTravelPlans(0)
                        .adSponsoredEmails(true)
                        .build());
        when(billingPlanService.hasActiveSubscription(argThat((User user) -> user != null && "weather-admin".equals(user.getId()))))
                .thenReturn(false);

        BillingStatusResponse response = useCase.getForUser("weather-admin");

        assertEquals("weather-admin", response.getUserId());
        assertEquals(BillingPlan.FREE, response.getPlan());
        assertEquals(0, response.getMaxTravelPlans());
        assertFalse(response.isActiveSubscription());
    }

    @Test
    void shouldReturnBillingStatusForPersistedUser() {
        User user = User.builder()
                .id("user-1")
                .stripeCustomerId("cus_123")
                .build();
        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));
        when(billingPlanService.resolveEntitlements(user)).thenReturn(BillingEntitlements.builder()
                .plan(BillingPlan.PLUS)
                .paidPlan(true)
                .maxActiveAlerts(10)
                .maxTravelPlans(3)
                .adSponsoredEmails(false)
                .build());
        when(billingPlanService.hasActiveSubscription(user)).thenReturn(true);

        BillingStatusResponse response = useCase.getForUser("user-1");

        assertEquals("user-1", response.getUserId());
        assertEquals(BillingPlan.PLUS, response.getPlan());
        assertEquals(3, response.getMaxTravelPlans());
        assertEquals("cus_123", response.getStripeCustomerId());
    }
}
