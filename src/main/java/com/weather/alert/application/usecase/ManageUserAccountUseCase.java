package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.ChannelVerificationResponse;
import com.weather.alert.application.dto.ChangePasswordRequest;
import com.weather.alert.application.dto.RegisterUserRequest;
import com.weather.alert.application.dto.RegisterUserResponse;
import com.weather.alert.application.dto.ResendRegistrationVerificationRequest;
import com.weather.alert.application.dto.UpdateMyAccountRequest;
import com.weather.alert.application.dto.UserAccountResponse;
import com.weather.alert.application.dto.VerifyRegistrationEmailRequest;
import com.weather.alert.application.exception.EmailAlreadyInUseException;
import com.weather.alert.application.exception.InvalidCurrentPasswordException;
import com.weather.alert.application.exception.InvalidUserAccountStateException;
import com.weather.alert.application.exception.UserAlreadyExistsException;
import com.weather.alert.application.exception.UserNotFoundException;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ManageUserAccountUseCase.class);

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManageChannelVerificationUseCase manageChannelVerificationUseCase;
    private final DeleteMyAccountUseCase deleteMyAccountUseCase;

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
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(false)
                .passwordResetRequired(false)
                .emailEnabled(true)
                .smsEnabled(request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank())
                .pushEnabled(false)
                .approvedAt(now)
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
            throw new InvalidUserAccountStateException("User does not have a registered email: " + userId);
        }

        com.weather.alert.application.dto.StartChannelVerificationRequest start =
                new com.weather.alert.application.dto.StartChannelVerificationRequest();
        start.setChannel(NotificationChannel.EMAIL);
        start.setDestination(normalizeEmail(user.getEmail()));
        return manageChannelVerificationUseCase.startVerification(userId, start);
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getMyAccount(String userId) {
        return userRepository.findById(userId)
                .map(UserAccountResponse::fromDomain)
                .orElseGet(() -> reservedAccountResponse(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId)));
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

    @Transactional
    public UserAccountResponse changeMyPassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetRequired(false);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("ACCOUNT_PASSWORD_CHANGED userId={}", userId);
        return UserAccountResponse.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public List<UserAccountResponse> listAllAccounts() {
        return userRepository.findAll().stream()
                .map(UserAccountResponse::fromDomain)
                .toList();
    }

    @Transactional
    public UserAccountResponse suspendAccount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.setApprovalStatus(UserApprovalStatus.SUSPENDED);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("ACCOUNT_SUSPENDED userId={}", userId);
        return UserAccountResponse.fromDomain(saved);
    }

    @Transactional
    public UserAccountResponse reactivateAccount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.setApprovalStatus(UserApprovalStatus.ACTIVE);
        if (user.getApprovedAt() == null) {
            user.setApprovedAt(Instant.now());
        }
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("ACCOUNT_REACTIVATED userId={}", userId);
        return UserAccountResponse.fromDomain(saved);
    }

    @Transactional
    public UserAccountResponse forcePasswordReset(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.setPasswordResetRequired(true);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("ACCOUNT_FORCE_PASSWORD_RESET userId={}", userId);
        return UserAccountResponse.fromDomain(saved);
    }

    @Transactional
    public void deleteMyAccount(String userId) {
        if (isReservedUsername(userId)) {
            throw new InvalidUserAccountStateException("Reserved bootstrap accounts cannot be deleted");
        }
        deleteMyAccountUseCase.delete(userId);
    }

    private boolean isReservedUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return username.equalsIgnoreCase(valueOrEmpty(bootstrapUserUsername))
                || username.equalsIgnoreCase(valueOrEmpty(bootstrapAdminUsername));
    }

    private java.util.Optional<UserAccountResponse> reservedAccountResponse(String userId) {
        if (userId == null || userId.isBlank()) {
            return java.util.Optional.empty();
        }
        if (userId.equalsIgnoreCase(valueOrEmpty(bootstrapAdminUsername))) {
            return java.util.Optional.of(UserAccountResponse.builder()
                    .id(userId)
                    .email(userId)
                    .name("SkyPanda Admin")
                    .role("ROLE_ADMIN")
                    .approvalStatus(UserApprovalStatus.ACTIVE)
                    .emailVerified(true)
                    .passwordResetRequired(false)
                    .build());
        }
        if (userId.equalsIgnoreCase(valueOrEmpty(bootstrapUserUsername))) {
            return java.util.Optional.of(UserAccountResponse.builder()
                    .id(userId)
                    .email(userId)
                    .name("SkyPanda User")
                    .role("ROLE_USER")
                    .approvalStatus(UserApprovalStatus.ACTIVE)
                    .emailVerified(true)
                    .passwordResetRequired(false)
                    .build());
        }
        return java.util.Optional.empty();
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
