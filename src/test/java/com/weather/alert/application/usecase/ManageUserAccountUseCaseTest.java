package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.ChannelVerificationResponse;
import com.weather.alert.application.dto.RegisterUserRequest;
import com.weather.alert.application.dto.ResendRegistrationVerificationRequest;
import com.weather.alert.application.dto.VerifyRegistrationEmailRequest;
import com.weather.alert.application.exception.InvalidAccountApprovalStateException;
import com.weather.alert.domain.model.ChannelVerificationStatus;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageUserAccountUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ManageChannelVerificationUseCase manageChannelVerificationUseCase;

    private ManageUserAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManageUserAccountUseCase(
                userRepository,
                passwordEncoder,
                manageChannelVerificationUseCase);
    }

    @Test
    void shouldRegisterPendingAccountAndStartEmailVerification() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername("alice");
        request.setPassword("StrongPass123!");
        request.setEmail("alice@example.com");
        request.setName("Alice");
        request.setPhoneNumber("+14075551234");

        when(userRepository.findById("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(manageChannelVerificationUseCase.startVerification(any(), any())).thenReturn(ChannelVerificationResponse.builder()
                .id("verification-1")
                .channel(NotificationChannel.EMAIL)
                .destination("alice@example.com")
                .status(ChannelVerificationStatus.PENDING_VERIFICATION)
                .verificationToken("token")
                .build());

        var response = useCase.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserApprovalStatus.PENDING_APPROVAL, userCaptor.getValue().getApprovalStatus());
        assertFalse(Boolean.TRUE.equals(userCaptor.getValue().getEmailVerified()));
        assertEquals("alice", response.getAccount().getId());
        assertNotNull(response.getEmailVerification());
    }

    @Test
    void shouldMarkEmailVerifiedAfterConfirmation() {
        VerifyRegistrationEmailRequest request = new VerifyRegistrationEmailRequest();
        request.setUserId("alice");
        request.setVerificationId("verification-1");
        request.setToken("token");

        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .approvalStatus(UserApprovalStatus.PENDING_APPROVAL)
                .emailVerified(false)
                .build()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.verifyRegisteredEmail(request);

        assertEquals("alice", response.getId());
        assertEquals(true, response.getEmailVerified());
        verify(manageChannelVerificationUseCase).confirmVerification(any(), any(), any());
    }

    @Test
    void shouldRequireVerifiedEmailBeforeApproval() {
        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .approvalStatus(UserApprovalStatus.PENDING_APPROVAL)
                .emailVerified(false)
                .build()));

        assertThrows(InvalidAccountApprovalStateException.class, () -> useCase.approveAccount("alice"));
    }

    @Test
    void shouldListOnlyPendingAccounts() {
        when(userRepository.findAll()).thenReturn(List.of(
                User.builder().id("pending").approvalStatus(UserApprovalStatus.PENDING_APPROVAL).build(),
                User.builder().id("active").approvalStatus(UserApprovalStatus.ACTIVE).build()));

        var pending = useCase.listPendingAccounts();

        assertEquals(1, pending.size());
        assertEquals("pending", pending.get(0).getId());
    }

    @Test
    void shouldResendRegistrationVerificationToken() {
        ResendRegistrationVerificationRequest request = new ResendRegistrationVerificationRequest();
        request.setUsername("alice");

        when(userRepository.findById("alice")).thenReturn(Optional.of(User.builder()
                .id("alice")
                .email("alice@example.com")
                .approvalStatus(UserApprovalStatus.PENDING_APPROVAL)
                .emailVerified(false)
                .build()));
        when(manageChannelVerificationUseCase.startVerification(eq("alice"), any())).thenReturn(ChannelVerificationResponse.builder()
                .id("verification-2")
                .channel(NotificationChannel.EMAIL)
                .destination("alice@example.com")
                .status(ChannelVerificationStatus.PENDING_VERIFICATION)
                .verificationToken("new-token")
                .build());

        ChannelVerificationResponse response = useCase.resendRegistrationEmailVerification(request);

        assertEquals("verification-2", response.getId());
        assertEquals("alice@example.com", response.getDestination());
        verify(manageChannelVerificationUseCase).startVerification(eq("alice"), any());
    }
}
