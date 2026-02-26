package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.ChannelVerificationResponse;
import com.weather.alert.application.dto.ConfirmChannelVerificationRequest;
import com.weather.alert.application.dto.StartChannelVerificationRequest;
import com.weather.alert.application.exception.ChannelDestinationInUseException;
import com.weather.alert.application.exception.InvalidVerificationTokenException;
import com.weather.alert.application.exception.UnsupportedNotificationChannelException;
import com.weather.alert.application.exception.VerificationDeliveryFailedException;
import com.weather.alert.application.exception.VerificationTokenExpiredException;
import com.weather.alert.domain.model.ChannelVerification;
import com.weather.alert.domain.model.ChannelVerificationStatus;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.ChannelVerificationRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageChannelVerificationUseCaseTest {

    @Mock
    private ChannelVerificationRepositoryPort channelVerificationRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private EmailSenderPort emailSenderPort;

    private ManageChannelVerificationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManageChannelVerificationUseCase(channelVerificationRepository, userRepository, emailSenderPort);
        ReflectionTestUtils.setField(useCase, "tokenTtlMinutes", 15L);
        ReflectionTestUtils.setField(useCase, "exposeRawToken", true);
        ReflectionTestUtils.setField(useCase, "sendEmail", false);
        ReflectionTestUtils.setField(useCase, "verificationEmailSubject", "Weather Alert email verification");
    }

    @Test
    void shouldStartEmailVerificationWithHashedToken() {
        StartChannelVerificationRequest request = new StartChannelVerificationRequest();
        request.setChannel(NotificationChannel.EMAIL);
        request.setDestination("Dev-Admin@Example.com");

        when(userRepository.findByEmail("dev-admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-admin")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-admin", NotificationChannel.EMAIL, "dev-admin@example.com")).thenReturn(Optional.empty());
        when(channelVerificationRepository.save(any(ChannelVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChannelVerificationResponse response = useCase.startVerification("dev-admin", request);

        assertNotNull(response.getId());
        assertEquals(ChannelVerificationStatus.PENDING_VERIFICATION, response.getStatus());
        assertEquals("dev-admin@example.com", response.getDestination());
        assertNotNull(response.getVerificationToken());

        verify(channelVerificationRepository).save(any(ChannelVerification.class));
    }

    @Test
    void shouldReturnExistingVerifiedRecordWithoutIssuingToken() {
        StartChannelVerificationRequest request = new StartChannelVerificationRequest();
        request.setChannel(NotificationChannel.EMAIL);
        request.setDestination("dev-admin@example.com");

        ChannelVerification verified = ChannelVerification.builder()
                .id("verification-1")
                .userId("dev-admin")
                .channel(NotificationChannel.EMAIL)
                .destination("dev-admin@example.com")
                .status(ChannelVerificationStatus.VERIFIED)
                .verifiedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("dev-admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-admin")).thenReturn(Optional.of(User.builder()
                .id("dev-admin")
                .email("dev-admin@example.com")
                .build()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-admin", NotificationChannel.EMAIL, "dev-admin@example.com")).thenReturn(Optional.of(verified));

        ChannelVerificationResponse response = useCase.startVerification("dev-admin", request);

        assertEquals(ChannelVerificationStatus.VERIFIED, response.getStatus());
        assertNull(response.getVerificationToken());
        verify(channelVerificationRepository, never()).save(any(ChannelVerification.class));
    }

    @Test
    void shouldConfirmVerificationWhenTokenMatches() {
        ConfirmChannelVerificationRequest request = new ConfirmChannelVerificationRequest();
        request.setToken("known-token");

        ChannelVerification pending = ChannelVerification.builder()
                .id("verification-1")
                .userId("dev-admin")
                .channel(NotificationChannel.EMAIL)
                .destination("dev-admin@example.com")
                .status(ChannelVerificationStatus.PENDING_VERIFICATION)
                .verificationTokenHash(hash("known-token"))
                .tokenExpiresAt(Instant.now().plusSeconds(300))
                .build();

        when(channelVerificationRepository.findById("verification-1")).thenReturn(Optional.of(pending));
        when(channelVerificationRepository.save(any(ChannelVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChannelVerificationResponse response = useCase.confirmVerification("dev-admin", "verification-1", request);

        assertEquals(ChannelVerificationStatus.VERIFIED, response.getStatus());
        verify(channelVerificationRepository).save(any(ChannelVerification.class));
    }

    @Test
    void shouldRejectInvalidToken() {
        ConfirmChannelVerificationRequest request = new ConfirmChannelVerificationRequest();
        request.setToken("bad-token");

        ChannelVerification pending = ChannelVerification.builder()
                .id("verification-1")
                .userId("dev-admin")
                .channel(NotificationChannel.EMAIL)
                .destination("dev-admin@example.com")
                .status(ChannelVerificationStatus.PENDING_VERIFICATION)
                .verificationTokenHash(hash("known-token"))
                .tokenExpiresAt(Instant.now().plusSeconds(300))
                .build();

        when(channelVerificationRepository.findById("verification-1")).thenReturn(Optional.of(pending));

        assertThrows(
                InvalidVerificationTokenException.class,
                () -> useCase.confirmVerification("dev-admin", "verification-1", request));
        verify(channelVerificationRepository, never()).save(any(ChannelVerification.class));
    }

    @Test
    void shouldMarkVerificationExpiredWhenTokenIsExpired() {
        ConfirmChannelVerificationRequest request = new ConfirmChannelVerificationRequest();
        request.setToken("known-token");

        ChannelVerification pending = ChannelVerification.builder()
                .id("verification-1")
                .userId("dev-admin")
                .channel(NotificationChannel.EMAIL)
                .destination("dev-admin@example.com")
                .status(ChannelVerificationStatus.PENDING_VERIFICATION)
                .verificationTokenHash(hash("known-token"))
                .tokenExpiresAt(Instant.now().minusSeconds(10))
                .build();

        when(channelVerificationRepository.findById("verification-1")).thenReturn(Optional.of(pending));
        when(channelVerificationRepository.save(any(ChannelVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
                VerificationTokenExpiredException.class,
                () -> useCase.confirmVerification("dev-admin", "verification-1", request));
        verify(channelVerificationRepository).save(any(ChannelVerification.class));
    }

    @Test
    void shouldRejectUnsupportedChannel() {
        StartChannelVerificationRequest request = new StartChannelVerificationRequest();
        request.setChannel(NotificationChannel.PUSH);
        request.setDestination("anything");

        assertThrows(
                UnsupportedNotificationChannelException.class,
                () -> useCase.startVerification("dev-admin", request));
    }

    @Test
    void shouldRejectEmailAlreadyUsedByAnotherUser() {
        StartChannelVerificationRequest request = new StartChannelVerificationRequest();
        request.setChannel(NotificationChannel.EMAIL);
        request.setDestination("shared@example.com");

        when(userRepository.findByEmail("shared@example.com")).thenReturn(Optional.of(User.builder()
                .id("other-user")
                .email("shared@example.com")
                .build()));

        assertThrows(
                ChannelDestinationInUseException.class,
                () -> useCase.startVerification("dev-admin", request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldStoreTokenHashNotRawToken() {
        StartChannelVerificationRequest request = new StartChannelVerificationRequest();
        request.setChannel(NotificationChannel.EMAIL);
        request.setDestination("dev-admin@example.com");

        when(userRepository.findByEmail("dev-admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-admin")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-admin", NotificationChannel.EMAIL, "dev-admin@example.com")).thenReturn(Optional.empty());
        when(channelVerificationRepository.save(any(ChannelVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChannelVerificationResponse response = useCase.startVerification("dev-admin", request);

        assertNotNull(response.getVerificationToken());
        assertTrue(response.getVerificationToken().length() > 20);
        ArgumentCaptor<ChannelVerification> captor = ArgumentCaptor.forClass(ChannelVerification.class);
        verify(channelVerificationRepository).save(captor.capture());
        assertNotNull(captor.getValue().getVerificationTokenHash());
        assertNotEquals(response.getVerificationToken(), captor.getValue().getVerificationTokenHash());
        assertEquals(hash(response.getVerificationToken()), captor.getValue().getVerificationTokenHash());
    }

    @Test
    void shouldSendVerificationEmailWhenEnabled() {
        ReflectionTestUtils.setField(useCase, "sendEmail", true);

        StartChannelVerificationRequest request = new StartChannelVerificationRequest();
        request.setChannel(NotificationChannel.EMAIL);
        request.setDestination("dev-admin@example.com");

        when(userRepository.findByEmail("dev-admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-admin")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-admin", NotificationChannel.EMAIL, "dev-admin@example.com")).thenReturn(Optional.empty());
        when(channelVerificationRepository.save(any(ChannelVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult("provider-id"));

        ChannelVerificationResponse response = useCase.startVerification("dev-admin", request);

        assertNotNull(response.getVerificationToken());
        verify(emailSenderPort).send(any());
    }

    @Test
    void shouldFailStartVerificationWhenEmailDeliveryFails() {
        ReflectionTestUtils.setField(useCase, "sendEmail", true);

        StartChannelVerificationRequest request = new StartChannelVerificationRequest();
        request.setChannel(NotificationChannel.EMAIL);
        request.setDestination("dev-admin@example.com");

        when(userRepository.findByEmail("dev-admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-admin")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-admin", NotificationChannel.EMAIL, "dev-admin@example.com")).thenReturn(Optional.empty());
        when(channelVerificationRepository.save(any(ChannelVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailSenderPort.send(any())).thenThrow(new EmailDeliveryException(
                DeliveryFailureType.RETRYABLE,
                "smtp timeout",
                new RuntimeException("smtp timeout")));

        assertThrows(
                VerificationDeliveryFailedException.class,
                () -> useCase.startVerification("dev-admin", request));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
