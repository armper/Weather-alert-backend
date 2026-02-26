package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.ChannelVerificationResponse;
import com.weather.alert.application.dto.RegisterUserRequest;
import com.weather.alert.application.dto.RegisterUserResponse;
import com.weather.alert.application.dto.ResendRegistrationVerificationRequest;
import com.weather.alert.application.dto.UpdateMyAccountRequest;
import com.weather.alert.application.dto.VerifyRegistrationEmailRequest;
import com.weather.alert.application.dto.UserAccountResponse;
import com.weather.alert.application.exception.EmailAlreadyInUseException;
import com.weather.alert.application.exception.InvalidAccountApprovalStateException;
import com.weather.alert.application.exception.UserAlreadyExistsException;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ManageUserAccountUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManageChannelVerificationUseCase manageChannelVerificationUseCase;

    @Value("${app.security.user.username:}")
    private String bootstrapUserUsername;

    @Value("${app.security.admin.username:}")
    private String bootstrapAdminUsername;

    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        String userId = normalizeUserId(request.getUsername());
        String email = normalizeEmail(request.getEmail());

        if (isReservedUsername(userId)) {
            throw new UserAlreadyExistsException(userId);
        }
        if (userRepository.findById(userId).isPresent()) {
            throw new UserAlreadyExistsException(userId);
        }
        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new EmailAlreadyInUseException(email);
        });

        Instant now = Instant.now();
        User saved = userRepository.save(User.builder()
                .id(userId)
                .email(email)
                .phoneNumber(normalizePhone(request.getPhoneNumber()))
                .name(normalizeName(request.getName(), userId))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .approvalStatus(UserApprovalStatus.PENDING_APPROVAL)
                .emailVerified(false)
                .emailEnabled(true)
                .smsEnabled(request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank())
                .pushEnabled(false)
                .createdAt(now)
                .updatedAt(now)
                .build());

        com.weather.alert.application.dto.StartChannelVerificationRequest start =
                new com.weather.alert.application.dto.StartChannelVerificationRequest();
        start.setChannel(NotificationChannel.EMAIL);
        start.setDestination(email);
        ChannelVerificationResponse verification =
                manageChannelVerificationUseCase.startVerification(userId, start);

        return RegisterUserResponse.builder()
                .account(UserAccountResponse.fromDomain(saved))
                .emailVerification(verification)
                .build();
    }

    @Transactional
    public UserAccountResponse verifyRegisteredEmail(VerifyRegistrationEmailRequest request) {
        com.weather.alert.application.dto.ConfirmChannelVerificationRequest confirm =
                new com.weather.alert.application.dto.ConfirmChannelVerificationRequest();
        confirm.setToken(request.getToken());
        manageChannelVerificationUseCase.confirmVerification(request.getUserId(), request.getVerificationId(), confirm);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));
        user.setEmailVerified(true);
        user.setEmailEnabled(true);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return UserAccountResponse.fromDomain(saved);
    }

    @Transactional
    public ChannelVerificationResponse resendRegistrationEmailVerification(
            ResendRegistrationVerificationRequest request) {
        String userId = normalizeUserId(request.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new InvalidAccountApprovalStateException("User does not have a registered email: " + userId);
        }

        com.weather.alert.application.dto.StartChannelVerificationRequest start =
                new com.weather.alert.application.dto.StartChannelVerificationRequest();
        start.setChannel(NotificationChannel.EMAIL);
        start.setDestination(normalizeEmail(user.getEmail()));
        return manageChannelVerificationUseCase.startVerification(userId, start);
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getMyAccount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return UserAccountResponse.fromDomain(user);
    }

    @Transactional
    public UserAccountResponse updateMyAccount(String userId, UpdateMyAccountRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (request.getName() != null) {
            user.setName(normalizeName(request.getName(), user.getId()));
        }
        if (request.getPhoneNumber() != null) {
            String normalizedPhone = normalizePhone(request.getPhoneNumber());
            user.setPhoneNumber(normalizedPhone);
            user.setSmsEnabled(normalizedPhone != null && !normalizedPhone.isBlank());
        }
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return UserAccountResponse.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public List<UserAccountResponse> listPendingAccounts() {
        return userRepository.findAll().stream()
                .filter(user -> user.getApprovalStatus() == UserApprovalStatus.PENDING_APPROVAL)
                .map(UserAccountResponse::fromDomain)
                .toList();
    }

    @Transactional
    public UserAccountResponse approveAccount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new InvalidAccountApprovalStateException(
                    "Cannot approve user before email verification: " + userId);
        }
        if (user.getApprovalStatus() == UserApprovalStatus.ACTIVE) {
            return UserAccountResponse.fromDomain(user);
        }
        user.setApprovalStatus(UserApprovalStatus.ACTIVE);
        user.setApprovedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return UserAccountResponse.fromDomain(saved);
    }

    private boolean isReservedUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return username.equalsIgnoreCase(valueOrEmpty(bootstrapUserUsername))
                || username.equalsIgnoreCase(valueOrEmpty(bootstrapAdminUsername));
    }

    private String normalizeUserId(String userId) {
        return userId == null ? null : userId.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String value = phoneNumber.trim();
        return value.isBlank() ? null : value;
    }

    private String normalizeName(String name, String fallbackUserId) {
        if (name == null) {
            return fallbackUserId;
        }
        String value = name.trim();
        return value.isBlank() ? fallbackUserId : value;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
