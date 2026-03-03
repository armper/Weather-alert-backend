package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.ConfirmPasswordResetRequest;
import com.weather.alert.application.dto.ConfirmUsernameRecoveryRequest;
import com.weather.alert.application.dto.ForgotPasswordRequest;
import com.weather.alert.application.dto.ForgotUsernameRequest;
import com.weather.alert.application.exception.InvalidRecoveryCodeException;
import com.weather.alert.application.service.AuthSecurityGuardService;
import com.weather.alert.domain.model.AccountRecoveryPurpose;
import com.weather.alert.domain.model.AccountRecoveryToken;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AccountRecoveryTokenRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageAccountRecoveryUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AccountRecoveryTokenRepositoryPort accountRecoveryTokenRepository;

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthSecurityGuardService authSecurityGuardService;

    private ManageAccountRecoveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManageAccountRecoveryUseCase(
                userRepository,
                accountRecoveryTokenRepository,
                emailSenderPort,
                passwordEncoder,
                authSecurityGuardService);
        ReflectionTestUtils.setField(useCase, "tokenTtlMinutes", 15L);
        ReflectionTestUtils.setField(useCase, "requestCooldownSeconds", 60L);
        ReflectionTestUtils.setField(useCase, "exposeRawCode", true);
        ReflectionTestUtils.setField(useCase, "sendRecoveryEmails", false);
        ReflectionTestUtils.setField(useCase, "recoveryFrontendBaseUrl", "http://localhost:5174");
        ReflectionTestUtils.setField(useCase, "usernameRecoveryEmailSubject", "Your Weather Alert username");
        ReflectionTestUtils.setField(useCase, "passwordRecoveryEmailSubject", "Reset your Weather Alert password");
    }

    @Test
    void shouldIssueUsernameRecoveryCodeForKnownEmail() {
        ForgotUsernameRequest request = new ForgotUsernameRequest();
        request.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .name("Alice")
                .build()));
        when(accountRecoveryTokenRepository.findLatestByUserIdAndPurpose("alice", AccountRecoveryPurpose.USERNAME_REMINDER))
                .thenReturn(Optional.empty());
        when(accountRecoveryTokenRepository.save(any(AccountRecoveryToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.requestUsernameReminder(request);

        assertNotNull(response.getRecoveryId());
        assertNotNull(response.getCodeExpiresAt());
        assertNotNull(response.getRecoveryCode());

        ArgumentCaptor<AccountRecoveryToken> tokenCaptor = ArgumentCaptor.forClass(AccountRecoveryToken.class);
        verify(accountRecoveryTokenRepository).save(tokenCaptor.capture());
        AccountRecoveryToken savedToken = tokenCaptor.getValue();
        assertEquals(AccountRecoveryPurpose.USERNAME_REMINDER, savedToken.getPurpose());
        assertEquals("alice", savedToken.getUserId());
        assertNotEquals(response.getRecoveryCode(), savedToken.getTokenHash());
        assertEquals(hash(response.getRecoveryCode()), savedToken.getTokenHash());
        verify(emailSenderPort, never()).send(any());
    }

    @Test
    void shouldSendFriendlyUsernameRecoveryEmailWithoutTechnicalPayload() {
        ReflectionTestUtils.setField(useCase, "sendRecoveryEmails", true);

        ForgotUsernameRequest request = new ForgotUsernameRequest();
        request.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .name("Alice")
                .build()));
        when(accountRecoveryTokenRepository.findLatestByUserIdAndPurpose("alice", AccountRecoveryPurpose.USERNAME_REMINDER))
                .thenReturn(Optional.empty());
        when(accountRecoveryTokenRepository.save(any(AccountRecoveryToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.requestUsernameReminder(request);

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(emailCaptor.capture());

        EmailMessage email = emailCaptor.getValue();
        assertEquals("alice@example.com", email.to());
        assertEquals("Your Weather Alert username", email.subject());
        assertTrue(email.body().contains(response.getRecoveryCode()));
        assertTrue(email.body().contains("This code expires in about 15 minutes."));
        assertTrue(email.body().contains("recoveryMode=username"));
        assertFalse(email.body().contains("Recovery ID:"));
        assertFalse(email.body().contains("POST /api/"));
    }

    @Test
    void shouldReturnSyntheticResponseForUnknownUsernameEmail() {
        ForgotUsernameRequest request = new ForgotUsernameRequest();
        request.setEmail("unknown@example.com");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        var response = useCase.requestUsernameReminder(request);

        assertNotNull(response.getRecoveryId());
        assertNotNull(response.getCodeExpiresAt());
        assertNotNull(response.getRecoveryCode());
        verify(accountRecoveryTokenRepository, never()).save(any());
    }

    @Test
    void shouldConfirmUsernameRecoveryAndMarkTokenUsed() {
        ConfirmUsernameRecoveryRequest request = new ConfirmUsernameRecoveryRequest();
        request.setRecoveryId("recovery-1");
        request.setCode("A2B3C4D5");

        AccountRecoveryToken token = AccountRecoveryToken.builder()
                .id("recovery-1")
                .userId("alice")
                .purpose(AccountRecoveryPurpose.USERNAME_REMINDER)
                .tokenHash(hash("A2B3C4D5"))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        when(accountRecoveryTokenRepository.findById("recovery-1")).thenReturn(Optional.of(token));
        when(accountRecoveryTokenRepository.save(any(AccountRecoveryToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder().id("alice").build()));

        var response = useCase.confirmUsernameReminder(request);

        assertEquals("alice", response.getUsername());
        verify(accountRecoveryTokenRepository).save(any(AccountRecoveryToken.class));
    }

    @Test
    void shouldResetPasswordWhenRecoveryCodeIsValid() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsernameOrEmail("alice");

        User user = User.builder()
                .id("alice")
                .email("alice@example.com")
                .passwordHash("old-hash")
                .build();

        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(accountRecoveryTokenRepository.findLatestByUserIdAndPurpose("alice", AccountRecoveryPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(accountRecoveryTokenRepository.save(any(AccountRecoveryToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var recovery = useCase.requestPasswordReset(request);

        ConfirmPasswordResetRequest confirmRequest = new ConfirmPasswordResetRequest();
        confirmRequest.setRecoveryId(recovery.getRecoveryId());
        confirmRequest.setCode(recovery.getRecoveryCode());
        confirmRequest.setNewPassword("NewStrongPass123!");

        when(accountRecoveryTokenRepository.findById(recovery.getRecoveryId())).thenReturn(Optional.of(AccountRecoveryToken.builder()
                .id(recovery.getRecoveryId())
                .userId("alice")
                .purpose(AccountRecoveryPurpose.PASSWORD_RESET)
                .tokenHash(hash(recovery.getRecoveryCode()))
                .expiresAt(Instant.now().plusSeconds(300))
                .build()));
        when(passwordEncoder.encode("NewStrongPass123!")).thenReturn("new-hash");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.confirmPasswordReset(confirmRequest);

        assertEquals("Password updated successfully.", response.getMessage());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("new-hash", userCaptor.getValue().getPasswordHash());
    }

    @Test
    void shouldSendFriendlyPasswordResetEmailWithoutTechnicalPayload() {
        ReflectionTestUtils.setField(useCase, "sendRecoveryEmails", true);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsernameOrEmail("alice");

        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .name("Alice")
                .build()));
        when(accountRecoveryTokenRepository.findLatestByUserIdAndPurpose("alice", AccountRecoveryPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(accountRecoveryTokenRepository.save(any(AccountRecoveryToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.requestPasswordReset(request);

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(emailCaptor.capture());

        EmailMessage email = emailCaptor.getValue();
        assertEquals("alice@example.com", email.to());
        assertEquals("Reset your Weather Alert password", email.subject());
        assertTrue(email.body().contains(response.getRecoveryCode()));
        assertTrue(email.body().contains("This code expires in about 15 minutes."));
        assertTrue(email.body().contains("recoveryMode=password"));
        assertFalse(email.body().contains("Recovery ID:"));
        assertFalse(email.body().contains("POST /api/"));
    }

    @Test
    void shouldRejectPasswordResetWhenCodeDoesNotMatch() {
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest();
        request.setRecoveryId("recovery-1");
        request.setCode("BADCODE99");
        request.setNewPassword("NewStrongPass123!");

        when(accountRecoveryTokenRepository.findById("recovery-1")).thenReturn(Optional.of(AccountRecoveryToken.builder()
                .id("recovery-1")
                .userId("alice")
                .purpose(AccountRecoveryPurpose.PASSWORD_RESET)
                .tokenHash(hash("GOODCODE9"))
                .expiresAt(Instant.now().plusSeconds(300))
                .build()));

        assertThrows(InvalidRecoveryCodeException.class, () -> useCase.confirmPasswordReset(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectExpiredRecoveryCode() {
        ConfirmUsernameRecoveryRequest request = new ConfirmUsernameRecoveryRequest();
        request.setRecoveryId("recovery-1");
        request.setCode("A2B3C4D5");

        when(accountRecoveryTokenRepository.findById("recovery-1")).thenReturn(Optional.of(AccountRecoveryToken.builder()
                .id("recovery-1")
                .userId("alice")
                .purpose(AccountRecoveryPurpose.USERNAME_REMINDER)
                .tokenHash(hash("A2B3C4D5"))
                .expiresAt(Instant.now().minusSeconds(5))
                .build()));

        assertThrows(InvalidRecoveryCodeException.class, () -> useCase.confirmUsernameReminder(request));
        verify(accountRecoveryTokenRepository, never()).save(any());
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
