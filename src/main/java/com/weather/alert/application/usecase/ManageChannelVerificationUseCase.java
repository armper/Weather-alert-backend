package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.ChannelVerificationResponse;
import com.weather.alert.application.dto.ConfirmChannelVerificationRequest;
import com.weather.alert.application.dto.StartChannelVerificationRequest;
import com.weather.alert.application.exception.ChannelDestinationInUseException;
import com.weather.alert.application.exception.ChannelVerificationNotFoundException;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.exception.InvalidVerificationTokenException;
import com.weather.alert.application.exception.UnsupportedNotificationChannelException;
import com.weather.alert.application.exception.VerificationTokenExpiredException;
import com.weather.alert.domain.model.ChannelVerification;
import com.weather.alert.domain.model.ChannelVerificationStatus;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.ChannelVerificationRepositoryPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageChannelVerificationUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final ChannelVerificationRepositoryPort channelVerificationRepository;
    private final UserRepositoryPort userRepository;

    @Value("${app.notification.verification.token-ttl-minutes:15}")
    private long tokenTtlMinutes;

    @Value("${app.notification.verification.expose-raw-token:true}")
    private boolean exposeRawToken;

    @Transactional
    public ChannelVerificationResponse startVerification(String userId, StartChannelVerificationRequest request) {
        requireUserId(userId);
        if (request.getChannel() != NotificationChannel.EMAIL) {
            throw new UnsupportedNotificationChannelException(request.getChannel());
        }

        String destination = normalizeDestination(request.getDestination());
        ensureUserRecordForEmail(userId, destination);

        Optional<ChannelVerification> existing = channelVerificationRepository
                .findByUserIdAndChannelAndDestination(userId, request.getChannel(), destination);
        if (existing.isPresent() && existing.get().getStatus() == ChannelVerificationStatus.VERIFIED) {
            return ChannelVerificationResponse.fromDomain(existing.get(), null);
        }

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);
        Instant now = Instant.now();
        ChannelVerification verification = existing
                .map(current -> {
                    current.setStatus(ChannelVerificationStatus.PENDING_VERIFICATION);
                    current.setVerificationTokenHash(tokenHash);
                    current.setTokenExpiresAt(now.plus(tokenTtlMinutes, ChronoUnit.MINUTES));
                    current.setVerifiedAt(null);
                    current.setUpdatedAt(now);
                    if (current.getCreatedAt() == null) {
                        current.setCreatedAt(now);
                    }
                    return current;
                })
                .orElseGet(() -> ChannelVerification.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .channel(request.getChannel())
                        .destination(destination)
                        .status(ChannelVerificationStatus.PENDING_VERIFICATION)
                        .verificationTokenHash(tokenHash)
                        .tokenExpiresAt(now.plus(tokenTtlMinutes, ChronoUnit.MINUTES))
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        ChannelVerification saved = channelVerificationRepository.save(verification);
        String token = exposeRawToken ? rawToken : null;
        return ChannelVerificationResponse.fromDomain(saved, token);
    }

    @Transactional
    public ChannelVerificationResponse confirmVerification(
            String userId,
            String verificationId,
            ConfirmChannelVerificationRequest request) {
        requireUserId(userId);
        ChannelVerification verification = channelVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ChannelVerificationNotFoundException(verificationId));
        enforceUserAccess(userId, verification);

        if (verification.getStatus() == ChannelVerificationStatus.VERIFIED) {
            return ChannelVerificationResponse.fromDomain(verification, null);
        }
        if (verification.getStatus() == ChannelVerificationStatus.EXPIRED) {
            throw new VerificationTokenExpiredException();
        }
        if (verification.getStatus() == ChannelVerificationStatus.REVOKED) {
            throw new InvalidVerificationTokenException();
        }

        Instant now = Instant.now();
        if (verification.getTokenExpiresAt() != null && verification.getTokenExpiresAt().isBefore(now)) {
            verification.setStatus(ChannelVerificationStatus.EXPIRED);
            verification.setUpdatedAt(now);
            channelVerificationRepository.save(verification);
            throw new VerificationTokenExpiredException();
        }

        if (!tokenMatches(request.getToken(), verification.getVerificationTokenHash())) {
            throw new InvalidVerificationTokenException();
        }

        verification.setStatus(ChannelVerificationStatus.VERIFIED);
        verification.setVerificationTokenHash(null);
        verification.setTokenExpiresAt(null);
        verification.setVerifiedAt(now);
        verification.setUpdatedAt(now);

        ChannelVerification saved = channelVerificationRepository.save(verification);
        markEmailAsVerified(saved.getUserId(), saved.getDestination(), now);
        return ChannelVerificationResponse.fromDomain(saved, null);
    }

    private void ensureUserRecordForEmail(String userId, String email) {
        Instant now = Instant.now();
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ChannelDestinationInUseException(NotificationChannel.EMAIL, email);
                });

        User user = userRepository.findById(userId).orElseGet(() -> User.builder()
                .id(userId)
                .createdAt(now)
                .role("ROLE_USER")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(false)
                .build());
        user.setEmail(email);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(userId);
        }
        if (user.getEmailEnabled() == null) {
            user.setEmailEnabled(Boolean.TRUE);
        }
        if (user.getSmsEnabled() == null) {
            user.setSmsEnabled(Boolean.FALSE);
        }
        if (user.getPushEnabled() == null) {
            user.setPushEnabled(Boolean.FALSE);
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    private void markEmailAsVerified(String userId, String verifiedEmail, Instant now) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional == null) {
            return;
        }
        userOptional.ifPresent(user -> {
            user.setEmail(verifiedEmail);
            user.setEmailVerified(true);
            user.setEmailEnabled(true);
            user.setUpdatedAt(now);
            userRepository.save(user);
        });
    }

    private void enforceUserAccess(String userId, ChannelVerification verification) {
        if (verification.getUserId() == null || !verification.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("You do not have access to this verification");
        }
    }

    private void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ForbiddenOperationException("Unable to resolve authenticated user");
        }
    }

    private String normalizeDestination(String destination) {
        return destination == null ? null : destination.trim().toLowerCase();
    }

    private String generateToken() {
        byte[] random = new byte[24];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String hashToken(String rawToken) {
        return HEX_FORMAT.formatHex(sha256(rawToken));
    }

    private boolean tokenMatches(String rawToken, String storedTokenHash) {
        if (rawToken == null || rawToken.isBlank() || storedTokenHash == null || storedTokenHash.isBlank()) {
            return false;
        }
        try {
            byte[] candidate = sha256(rawToken);
            byte[] expected = HEX_FORMAT.parseHex(storedTokenHash);
            return MessageDigest.isEqual(candidate, expected);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
