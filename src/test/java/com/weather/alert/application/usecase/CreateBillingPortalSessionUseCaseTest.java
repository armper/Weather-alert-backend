package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBillingPortalSessionUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private BillingProviderPort billingProviderPort;

    @InjectMocks
    private CreateBillingPortalSessionUseCase useCase;

    @Test
    void shouldCreatePortalSessionForExistingStripeCustomer() {
        User user = User.builder()
                .id("user-1")
                .stripeCustomerId("cus_123")
                .build();

        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));
        when(billingProviderPort.createCustomerPortalSession("cus_123"))
                .thenReturn(BillingCheckoutSession.builder()
                        .id("bps_123")
                        .url("https://billing.stripe.com/p/session/bps_123")
                        .build());

        BillingCheckoutSessionResponse response = useCase.createForUser("user-1");

        verify(billingProviderPort).createCustomerPortalSession("cus_123");
        assertEquals("bps_123", response.getSessionId());
        assertEquals("https://billing.stripe.com/p/session/bps_123", response.getUrl());
    }

    @Test
    void shouldRejectPortalSessionWhenCustomerRecordIsMissing() {
        User user = User.builder()
                .id("user-1")
                .stripeCustomerId("")
                .build();

        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));

        assertThrows(BillingStateException.class, () -> useCase.createForUser("user-1"));
    }
}
