package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.ConfirmPasswordResetRequest;
import com.weather.alert.application.dto.ConfirmUsernameRecoveryRequest;
import com.weather.alert.application.dto.ForgotPasswordRequest;
import com.weather.alert.application.dto.ForgotUsernameRequest;
import com.weather.alert.application.dto.MagicLinkRequest;
import com.weather.alert.application.dto.MessageResponse;
import com.weather.alert.application.dto.RecoveryRequestResponse;
import com.weather.alert.application.dto.UsernameRecoveryResponse;
import com.weather.alert.application.dto.ConfirmMagicLinkRequest;
import com.weather.alert.application.exception.InvalidRecoveryCodeException;
import com.weather.alert.application.service.AuthSecurityGuardService;
import com.weather.alert.domain.model.AccountRecoveryPurpose;
import com.weather.alert.domain.model.AccountRecoveryToken;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AccountRecoveryTokenRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageAccountRecoveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageAccountRecoveryUseCase.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final UserRepositoryPort userRepository;
    private final AccountRecoveryTokenRepositoryPort accountRecoveryTokenRepository;
    private final EmailSenderPort emailSenderPort;
    private final PasswordEncoder passwordEncoder;
    private final AuthSecurityGuardService authSecurityGuardService;
    private final AuthenticateRegisteredUserUseCase authenticateRegisteredUserUseCase;

    @Value("${app.auth.recovery.token-ttl-minutes:15}")
    private long tokenTtlMinutes;

    @Value("${app.auth.recovery.request-cooldown-seconds:60}")
    private long requestCooldownSeconds;

    @Value("${app.auth.recovery.expose-raw-code:true}")
    private boolean exposeRawCode;

    @Value("${app.auth.recovery.send-email:false}")
    private boolean sendRecoveryEmails;

    @Value("${app.auth.recovery.frontend-base-url:http://localhost:5174}")
    private String recoveryFrontendBaseUrl;

    @Value("${app.auth.recovery.username-email-subject:Your Weather Alert username}")
    private String usernameRecoveryEmailSubject;

    @Value("${app.auth.recovery.password-email-subject:Reset your Weather Alert password}")
    private String passwordRecoveryEmailSubject;

    @Value("${app.auth.recovery.magic-link-email-subject:Your SkyPanda sign-in link}")
    private String magicLinkEmailSubject;

    @Transactional
    public RecoveryRequestResponse requestUsernameReminder(ForgotUsernameRequest request) {
        return requestUsernameReminder(request, "unknown");
    }

    @Transactional
    public RecoveryRequestResponse requestUsernameReminder(ForgotUsernameRequest request, String clientIp) {
        String email = normalizeEmail(request.getEmail());
        authSecurityGuardService.consumeRecoveryRequestQuota(AccountRecoveryPurpose.USERNAME_REMINDER, email, clientIp);

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.info("AUTH_RECOVERY_USERNAME_REQUEST user=unknown ip={}", valueOrUnknown(clientIp));
            return buildSyntheticRequestResponse("If an account exists, recovery instructions were sent.");
        }

        User user = userOptional.get();
        RecoveryRequestResponse cooldown = recoveryCooldownResponse(user, AccountRecoveryPurpose.USERNAME_REMINDER);
        if (cooldown != null) {
            log.info("AUTH_RECOVERY_USERNAME_REQUEST_THROTTLED userId={} ip={}", user.getId(), valueOrUnknown(clientIp));
            return cooldown;
        }

        RecoveryIssue issued = issueRecovery(user, AccountRecoveryPurpose.USERNAME_REMINDER);
        sendUsernameReminderEmail(user, issued);

        log.info("AUTH_RECOVERY_USERNAME_REQUEST userId={} ip={}", user.getId(), valueOrUnknown(clientIp));
        return RecoveryRequestResponse.builder()
                .message("If an account exists, recovery instructions were sent.")
                .recoveryId(issued.token().getId())
                .codeExpiresAt(issued.token().getExpiresAt())
                .recoveryCode(exposeRawCode ? issued.rawCode() : null)
                .build();
    }

    @Transactional
    public UsernameRecoveryResponse confirmUsernameReminder(ConfirmUsernameRecoveryRequest request) {
        return confirmUsernameReminder(request, "unknown");
    }

    @Transactional
    public UsernameRecoveryResponse confirmUsernameReminder(ConfirmUsernameRecoveryRequest request, String clientIp) {
        authSecurityGuardService.assertRecoveryConfirmAllowed(request.getRecoveryId(), clientIp);

        AccountRecoveryToken token;
        try {
            token = validateCode(
                    request.getRecoveryId(),
                    request.getCode(),
                    AccountRecoveryPurpose.USERNAME_REMINDER);
        } catch (InvalidRecoveryCodeException ex) {
            authSecurityGuardService.recordRecoveryConfirmFailure(request.getRecoveryId(), clientIp);
            log.warn("AUTH_RECOVERY_USERNAME_CONFIRM_FAILURE recoveryId={} ip={}", request.getRecoveryId(), valueOrUnknown(clientIp));
            throw ex;
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidRecoveryCodeException::new);
        markUsed(token);
        authSecurityGuardService.clearRecoveryConfirmFailures(request.getRecoveryId(), clientIp);
        log.info("AUTH_RECOVERY_USERNAME_CONFIRM_SUCCESS userId={} ip={}", user.getId(), valueOrUnknown(clientIp));

        return UsernameRecoveryResponse.builder()
                .message("Username recovered successfully.")
                .username(user.getId())
                .build();
    }

    @Transactional
    public RecoveryRequestResponse requestPasswordReset(ForgotPasswordRequest request) {
        return requestPasswordReset(request, "unknown");
    }

    @Transactional
    public RecoveryRequestResponse requestPasswordReset(ForgotPasswordRequest request, String clientIp) {
        String userKey = normalize(request.getUsernameOrEmail());
        authSecurityGuardService.consumeRecoveryRequestQuota(AccountRecoveryPurpose.PASSWORD_RESET, userKey, clientIp);

        Optional<User> userOptional = resolveUser(request.getUsernameOrEmail());
        if (userOptional.isEmpty()) {
            log.info("AUTH_RECOVERY_PASSWORD_REQUEST user=unknown ip={}", valueOrUnknown(clientIp));
            return buildSyntheticRequestResponse("If an account exists, recovery instructions were sent.");
        }

        User user = userOptional.get();
        RecoveryRequestResponse cooldown = recoveryCooldownResponse(user, AccountRecoveryPurpose.PASSWORD_RESET);
        if (cooldown != null) {
            log.info("AUTH_RECOVERY_PASSWORD_REQUEST_THROTTLED userId={} ip={}", user.getId(), valueOrUnknown(clientIp));
            return cooldown;
        }

        RecoveryIssue issued = issueRecovery(user, AccountRecoveryPurpose.PASSWORD_RESET);
        sendPasswordResetEmail(user, issued);

        log.info("AUTH_RECOVERY_PASSWORD_REQUEST userId={} ip={}", user.getId(), valueOrUnknown(clientIp));
        return RecoveryRequestResponse.builder()
                .message("If an account exists, recovery instructions were sent.")
                .recoveryId(issued.token().getId())
                .codeExpiresAt(issued.token().getExpiresAt())
                .recoveryCode(exposeRawCode ? issued.rawCode() : null)
                .build();
    }

    @Transactional
    public MessageResponse confirmPasswordReset(ConfirmPasswordResetRequest request) {
        return confirmPasswordReset(request, "unknown");
    }

    @Transactional
    public MessageResponse confirmPasswordReset(ConfirmPasswordResetRequest request, String clientIp) {
        authSecurityGuardService.assertRecoveryConfirmAllowed(request.getRecoveryId(), clientIp);

        AccountRecoveryToken token;
        try {
            token = validateCode(
                    request.getRecoveryId(),
                    request.getCode(),
                    AccountRecoveryPurpose.PASSWORD_RESET);
        } catch (InvalidRecoveryCodeException ex) {
            authSecurityGuardService.recordRecoveryConfirmFailure(request.getRecoveryId(), clientIp);
            log.warn("AUTH_RECOVERY_PASSWORD_CONFIRM_FAILURE recoveryId={} ip={}", request.getRecoveryId(), valueOrUnknown(clientIp));
            throw ex;
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidRecoveryCodeException::new);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetRequired(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        markUsed(token);
        authSecurityGuardService.clearRecoveryConfirmFailures(request.getRecoveryId(), clientIp);
        log.info("AUTH_RECOVERY_PASSWORD_CONFIRM_SUCCESS userId={} ip={}", user.getId(), valueOrUnknown(clientIp));

        return MessageResponse.builder()
                .message("Password updated successfully.")
                .build();
    }

    @Transactional
    public RecoveryRequestResponse requestMagicLogin(MagicLinkRequest request) {
        return requestMagicLogin(request, "unknown");
    }

    @Transactional
    public RecoveryRequestResponse requestMagicLogin(MagicLinkRequest request, String clientIp) {
        String userKey = normalize(request.getUsernameOrEmail());
        authSecurityGuardService.consumeRecoveryRequestQuota(AccountRecoveryPurpose.MAGIC_LOGIN, userKey, clientIp);

        Optional<User> userOptional = resolveUser(request.getUsernameOrEmail());
        if (userOptional.isEmpty()) {
            log.info("AUTH_MAGIC_LINK_REQUEST user=unknown ip={}", valueOrUnknown(clientIp));
            return buildSyntheticRequestResponse("If an account exists, a sign-in link was sent.");
        }

        User user = userOptional.get();
        RecoveryRequestResponse cooldown = recoveryCooldownResponse(user, AccountRecoveryPurpose.MAGIC_LOGIN);
        if (cooldown != null) {
            log.info("AUTH_MAGIC_LINK_REQUEST_THROTTLED userId={} ip={}", user.getId(), valueOrUnknown(clientIp));
            return cooldown;
        }

        RecoveryIssue issued = issueRecovery(user, AccountRecoveryPurpose.MAGIC_LOGIN);
        sendMagicLinkEmail(user, issued);

        log.info("AUTH_MAGIC_LINK_REQUEST userId={} ip={}", user.getId(), valueOrUnknown(clientIp));
        return RecoveryRequestResponse.builder()
                .message("If an account exists, a sign-in link was sent.")
                .recoveryId(issued.token().getId())
                .codeExpiresAt(issued.token().getExpiresAt())
                .recoveryCode(exposeRawCode ? issued.rawCode() : null)
                .build();
    }

    @Transactional
    public Authentication confirmMagicLogin(ConfirmMagicLinkRequest request) {
        return confirmMagicLogin(request, "unknown");
    }

    @Transactional
    public Authentication confirmMagicLogin(ConfirmMagicLinkRequest request, String clientIp) {
        authSecurityGuardService.assertRecoveryConfirmAllowed(request.getRecoveryId(), clientIp);

        AccountRecoveryToken token;
        try {
            token = validateCode(
                    request.getRecoveryId(),
                    request.getCode(),
                    AccountRecoveryPurpose.MAGIC_LOGIN);
        } catch (InvalidRecoveryCodeException ex) {
            authSecurityGuardService.recordRecoveryConfirmFailure(request.getRecoveryId(), clientIp);
            log.warn("AUTH_MAGIC_LINK_CONFIRM_FAILURE recoveryId={} ip={}", request.getRecoveryId(), valueOrUnknown(clientIp));
            throw ex;
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidRecoveryCodeException::new);
        Authentication authentication = authenticateRegisteredUserUseCase.toAuthentication(user);
        markUsed(token);
        authSecurityGuardService.clearRecoveryConfirmFailures(request.getRecoveryId(), clientIp);
        log.info("AUTH_MAGIC_LINK_CONFIRM_SUCCESS userId={} ip={}", user.getId(), valueOrUnknown(clientIp));
        return authentication;
    }

    private RecoveryRequestResponse recoveryCooldownResponse(User user, AccountRecoveryPurpose purpose) {
        Instant now = Instant.now();

        Optional<AccountRecoveryToken> latest = accountRecoveryTokenRepository.findLatestByUserIdAndPurpose(user.getId(), purpose)
                .filter(existing -> existing.getUsedAt() == null)
                .filter(existing -> existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now))
                .filter(existing -> existing.getCreatedAt() != null)
                .filter(existing -> existing.getCreatedAt().plusSeconds(requestCooldownSeconds).isAfter(now));

        if (latest.isEmpty()) {
            return null;
        }

        AccountRecoveryToken existing = latest.get();
        long retryAfterSeconds = ChronoUnit.SECONDS.between(now, existing.getCreatedAt().plusSeconds(requestCooldownSeconds));
        return RecoveryRequestResponse.builder()
                .message("A recovery code was already sent recently. Please wait before requesting another.")
                .recoveryId(existing.getId())
                .codeExpiresAt(existing.getExpiresAt())
                .retryAfterSeconds(Math.max(retryAfterSeconds, 1))
                .build();
    }

    private RecoveryIssue issueRecovery(User user, AccountRecoveryPurpose purpose) {
        String rawCode = generateCode(8);
        Instant now = Instant.now();
        AccountRecoveryToken token = accountRecoveryTokenRepository.save(AccountRecoveryToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .purpose(purpose)
                .tokenHash(hashCode(rawCode))
                .expiresAt(now.plus(tokenTtlMinutes, ChronoUnit.MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build());

        return new RecoveryIssue(token, rawCode);
    }

    private AccountRecoveryToken validateCode(String recoveryId, String code, AccountRecoveryPurpose purpose) {
        AccountRecoveryToken token = accountRecoveryTokenRepository.findById(recoveryId)
                .orElseThrow(InvalidRecoveryCodeException::new);

        if (token.getPurpose() != purpose) {
            throw new InvalidRecoveryCodeException();
        }
        if (token.getUsedAt() != null) {
            throw new InvalidRecoveryCodeException();
        }

        Instant now = Instant.now();
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(now)) {
            throw new InvalidRecoveryCodeException();
        }

        if (!matches(code, token.getTokenHash())) {
            throw new InvalidRecoveryCodeException();
        }

        return token;
    }

    private void markUsed(AccountRecoveryToken token) {
        Instant now = Instant.now();
        token.setUsedAt(now);
        token.setUpdatedAt(now);
        accountRecoveryTokenRepository.save(token);
    }

    private RecoveryRequestResponse buildSyntheticRequestResponse(String message) {
        Instant expiresAt = Instant.now().plus(tokenTtlMinutes, ChronoUnit.MINUTES);
        return RecoveryRequestResponse.builder()
                .message(message)
                .recoveryId(UUID.randomUUID().toString())
                .codeExpiresAt(expiresAt)
                .recoveryCode(exposeRawCode ? generateCode(8) : null)
                .build();
    }

    private Optional<User> resolveUser(String usernameOrEmail) {
        String value = normalize(usernameOrEmail);
        if (value == null) {
            return Optional.empty();
        }

        Optional<User> byId = userRepository.findById(value);
        if (byId.isPresent()) {
            return byId;
        }

        return userRepository.findByEmail(normalizeEmail(value));
    }

    private void sendUsernameReminderEmail(User user, RecoveryIssue issued) {
        if (!sendRecoveryEmails) {
            return;
        }

        String link = buildRecoveryLandingLink("username", issued.token().getId(), issued.rawCode());
        String expiryText = recoveryExpiryText();
        String linkInstruction = link == null
                ? "Open the Weather Alert app and enter the code in Forgot Username."
                : "Use this secure link:\n%s".formatted(link);

        String body = """
                Hi %s,

                We received a request to recover your username.

                Use this recovery code:
                %s

                %s

                This code expires in %s.

                If you did not request this, you can ignore this email.

                - Weather Alert Team
                """
                .formatted(
                        displayName(user),
                        issued.rawCode(),
                        linkInstruction,
                        expiryText);

        sendRecoveryEmail(user.getEmail(), usernameRecoveryEmailSubject, body);
    }

    private void sendPasswordResetEmail(User user, RecoveryIssue issued) {
        if (!sendRecoveryEmails) {
            return;
        }

        String link = buildRecoveryLandingLink("password", issued.token().getId(), issued.rawCode());
        String expiryText = recoveryExpiryText();
        String linkInstruction = link == null
                ? "Open the Weather Alert app and enter the code in Forgot Password."
                : "Use this secure link:\n%s".formatted(link);

        String body = """
                Hi %s,

                We received a request to reset your password.

                Use this recovery code:
                %s

                %s

                This code expires in %s.

                If you did not request this, you can ignore this email.

                - Weather Alert Team
                """
                .formatted(
                        displayName(user),
                        issued.rawCode(),
                        linkInstruction,
                        expiryText);

        sendRecoveryEmail(user.getEmail(), passwordRecoveryEmailSubject, body);
    }

    private void sendMagicLinkEmail(User user, RecoveryIssue issued) {
        if (!sendRecoveryEmails) {
            return;
        }

        String link = buildMagicLink(issued.token().getId(), issued.rawCode());
        String expiryText = recoveryExpiryText();

        String body = """
                Hi %s,

                Use this secure link to sign in to SkyPanda:
                %s

                If you requested the link on the same device and need the backup code, enter:
                %s

                This sign-in link expires in %s and can only be used once.

                If you did not request this, you can ignore this email.

                - SkyPanda
                """
                .formatted(
                        displayName(user),
                        link == null ? "Open the SkyPanda sign-in page and request a new magic link." : link,
                        issued.rawCode(),
                        expiryText);

        sendRecoveryEmail(user.getEmail(), magicLinkEmailSubject, body);
    }

    private String recoveryExpiryText() {
        long ttl = Math.max(tokenTtlMinutes, 1);
        return ttl == 1 ? "about 1 minute" : "about %d minutes".formatted(ttl);
    }

    private String buildRecoveryLandingLink(String mode, String recoveryId, String recoveryCode) {
        String base = normalize(recoveryFrontendBaseUrl);
        if (base == null) {
            return null;
        }

        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase
                + "/?recoveryMode=" + encode(mode)
                + "&recoveryId=" + encode(recoveryId)
                + "&recoveryCode=" + encode(recoveryCode);
    }

    private String buildMagicLink(String recoveryId, String recoveryCode) {
        String base = normalize(recoveryFrontendBaseUrl);
        if (base == null) {
            return null;
        }

        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase
                + "/auth/login?authMode=magic-link"
                + "&recoveryId=" + encode(recoveryId)
                + "&recoveryCode=" + encode(recoveryCode);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void sendRecoveryEmail(String to, String subject, String body) {
        try {
            emailSenderPort.send(EmailMessage.builder()
                    .to(to)
                    .subject(subject)
                    .body(body)
                    .build());
        } catch (RuntimeException ex) {
            log.warn("Failed to send recovery email to {}", to, ex);
        }
    }

    private String displayName(User user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        return user.getId();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private String hashCode(String rawCode) {
        return HEX_FORMAT.formatHex(sha256(rawCode));
    }

    private boolean matches(String rawCode, String storedHash) {
        if (rawCode == null || rawCode.isBlank() || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        try {
            byte[] candidate = sha256(rawCode);
            byte[] expected = HEX_FORMAT.parseHex(storedHash);
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

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(CODE_ALPHABET.length());
            builder.append(CODE_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private record RecoveryIssue(AccountRecoveryToken token, String rawCode) {
    }
}
