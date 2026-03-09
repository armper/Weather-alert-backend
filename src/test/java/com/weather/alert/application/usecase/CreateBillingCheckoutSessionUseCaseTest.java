package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.domain.model.BillingCheckoutSession;
import com.weather.alert.domain.model.BillingCheckoutSessionRequest;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.BillingProviderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBillingCheckoutSessionUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private BillingProviderPort billingProviderPort;

    @InjectMocks
    private CreateBillingCheckoutSessionUseCase useCase;

    @Test
    void shouldCreateCheckoutSessionForUserWithoutActiveSubscription() {
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .stripeCustomerId("cus_existing")
                .stripeSubscriptionStatus("canceled")
                .build();

        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));
        when(billingProviderPort.createSubscriptionCheckoutSession(org.mockito.ArgumentMatchers.any()))
                .thenReturn(BillingCheckoutSession.builder()
                        .id("cs_test_123")
                        .url("https://checkout.stripe.com/c/pay/cs_test_123")
                        .build());

        BillingCheckoutSessionResponse response = useCase.createForUser("user-1");

        ArgumentCaptor<BillingCheckoutSessionRequest> requestCaptor = ArgumentCaptor.forClass(BillingCheckoutSessionRequest.class);
        verify(billingProviderPort).createSubscriptionCheckoutSession(requestCaptor.capture());

        assertEquals("cs_test_123", response.getSessionId());
        assertEquals("https://checkout.stripe.com/c/pay/cs_test_123", response.getUrl());
        assertEquals("user-1", requestCaptor.getValue().getUserId());
        assertEquals("user@example.com", requestCaptor.getValue().getEmail());
        assertEquals("cus_existing", requestCaptor.getValue().getStripeCustomerId());
    }

    @Test
    void shouldRejectCheckoutWhenUserAlreadyHasActiveSubscription() {
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .stripeSubscriptionStatus("active")
                .build();

        when(userRepositoryPort.findById("user-1")).thenReturn(Optional.of(user));

        assertThrows(BillingStateException.class, () -> useCase.createForUser("user-1"));
    }
}
