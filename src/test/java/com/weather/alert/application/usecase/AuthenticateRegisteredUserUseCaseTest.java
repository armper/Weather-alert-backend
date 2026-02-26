package com.weather.alert.application.usecase;

import com.weather.alert.application.exception.EmailVerificationRequiredException;
import com.weather.alert.application.exception.UserApprovalRequiredException;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateRegisteredUserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticateRegisteredUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateRegisteredUserUseCase(userRepository, passwordEncoder);
    }

    @Test
    void shouldAuthenticateActiveVerifiedUser() {
        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .passwordHash("hash")
                .role("ROLE_USER")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(true)
                .build()));
        when(passwordEncoder.matches("StrongPass123!", "hash")).thenReturn(true);

        Optional<Authentication> authentication = useCase.authenticate("alice", "StrongPass123!");

        assertTrue(authentication.isPresent());
        assertEquals("alice", authentication.get().getName());
        assertEquals("ROLE_USER", authentication.get().getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldRejectPendingUser() {
        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .passwordHash("hash")
                .approvalStatus(UserApprovalStatus.PENDING_APPROVAL)
                .emailVerified(true)
                .build()));
        when(passwordEncoder.matches("StrongPass123!", "hash")).thenReturn(true);

        assertThrows(UserApprovalRequiredException.class, () -> useCase.authenticate("alice", "StrongPass123!"));
    }

    @Test
    void shouldRejectUnverifiedEmail() {
        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .passwordHash("hash")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(false)
                .build()));
        when(passwordEncoder.matches("StrongPass123!", "hash")).thenReturn(true);

        assertThrows(EmailVerificationRequiredException.class, () -> useCase.authenticate("alice", "StrongPass123!"));
    }
}

