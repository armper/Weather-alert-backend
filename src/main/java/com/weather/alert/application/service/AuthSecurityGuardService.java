package com.weather.alert.application.service;

import com.weather.alert.application.exception.TooManyRequestsException;
import com.weather.alert.domain.model.AccountRecoveryPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSecurityGuardService {

    private static final String UNKNOWN = "unknown";

    private final Map<String, FailureState> loginFailures = new ConcurrentHashMap<>();
    private final Map<String, RequestState> recoveryRequests = new ConcurrentHashMap<>();
    private final Map<String, FailureState> recoveryConfirmFailures = new ConcurrentHashMap<>();

    private final int loginMaxFailures;
    private final long loginWindowMinutes;
    private final long loginLockMinutes;

    private final int recoveryRequestMax;
    private final long recoveryRequestWindowMinutes;

    private final int recoveryConfirmMaxFailures;
    private final long recoveryConfirmWindowMinutes;
    private final long recoveryConfirmLockMinutes;

    public AuthSecurityGuardService(
            @Value("${app.auth.security.login.max-failures:6}") int loginMaxFailures,
            @Value("${app.auth.security.login.window-minutes:15}") long loginWindowMinutes,
            @Value("${app.auth.security.login.lock-minutes:15}") long loginLockMinutes,
            @Value("${app.auth.security.recovery.request.max-per-window:8}") int recoveryRequestMax,
            @Value("${app.auth.security.recovery.request.window-minutes:15}") long recoveryRequestWindowMinutes,
            @Value("${app.auth.security.recovery.confirm.max-failures:6}") int recoveryConfirmMaxFailures,
            @Value("${app.auth.security.recovery.confirm.window-minutes:15}") long recoveryConfirmWindowMinutes,
            @Value("${app.auth.security.recovery.confirm.lock-minutes:15}") long recoveryConfirmLockMinutes) {
        this.loginMaxFailures = loginMaxFailures;
        this.loginWindowMinutes = loginWindowMinutes;
        this.loginLockMinutes = loginLockMinutes;
        this.recoveryRequestMax = recoveryRequestMax;
        this.recoveryRequestWindowMinutes = recoveryRequestWindowMinutes;
        this.recoveryConfirmMaxFailures = recoveryConfirmMaxFailures;
        this.recoveryConfirmWindowMinutes = recoveryConfirmWindowMinutes;
        this.recoveryConfirmLockMinutes = recoveryConfirmLockMinutes;
    }

    public void assertLoginAllowed(String username, String clientIp) {
        String key = "login|" + normalize(username) + "|" + normalize(clientIp);
        assertFailureStateAllowed(
                loginFailures,
                key,
                "LOGIN_TEMPORARILY_LOCKED",
                "Too many failed sign-in attempts. Try again later.");
    }

    public void recordLoginFailure(String username, String clientIp) {
        String key = "login|" + normalize(username) + "|" + normalize(clientIp);
        recordFailure(
                loginFailures,
                key,
                loginMaxFailures,
                loginWindowMinutes,
                loginLockMinutes);
    }

    public void clearLoginFailures(String username, String clientIp) {
        String key = "login|" + normalize(username) + "|" + normalize(clientIp);
        loginFailures.remove(key);
    }

    public void consumeRecoveryRequestQuota(AccountRecoveryPurpose purpose, String identifier, String clientIp) {
        String key = "recovery-request|" + purpose.name() + "|" + normalize(identifier) + "|" + normalize(clientIp);
        Instant now = Instant.now();

        recoveryRequests.compute(key, (k, current) -> {
            if (current == null || current.windowStart.isBefore(now.minus(recoveryRequestWindowMinutes, ChronoUnit.MINUTES))) {
                return new RequestState(now, 1);
            }
            if (current.requestCount >= recoveryRequestMax) {
                long retryAfterSeconds = ChronoUnit.SECONDS.between(now, current.windowStart.plus(recoveryRequestWindowMinutes, ChronoUnit.MINUTES));
                throw new TooManyRequestsException(
                        "RECOVERY_REQUEST_RATE_LIMITED",
                        "Too many recovery requests. Try again in " + Math.max(retryAfterSeconds, 1) + " seconds.");
            }
            current.requestCount += 1;
            return current;
        });
    }

    public void assertRecoveryConfirmAllowed(String recoveryId, String clientIp) {
        String key = "recovery-confirm|" + normalize(recoveryId) + "|" + normalize(clientIp);
        assertFailureStateAllowed(
                recoveryConfirmFailures,
                key,
                "RECOVERY_CONFIRM_TEMPORARILY_LOCKED",
                "Too many invalid recovery code attempts. Try again later.");
    }

    public void recordRecoveryConfirmFailure(String recoveryId, String clientIp) {
        String key = "recovery-confirm|" + normalize(recoveryId) + "|" + normalize(clientIp);
        recordFailure(
                recoveryConfirmFailures,
                key,
                recoveryConfirmMaxFailures,
                recoveryConfirmWindowMinutes,
                recoveryConfirmLockMinutes);
    }

    public void clearRecoveryConfirmFailures(String recoveryId, String clientIp) {
        String key = "recovery-confirm|" + normalize(recoveryId) + "|" + normalize(clientIp);
        recoveryConfirmFailures.remove(key);
    }

    private void assertFailureStateAllowed(
            Map<String, FailureState> states,
            String key,
            String errorCode,
            String message) {
        FailureState state = states.get(key);
        if (state == null || state.lockedUntil == null) {
            return;
        }
        if (Instant.now().isBefore(state.lockedUntil)) {
            throw new TooManyRequestsException(errorCode, message);
        }
        states.remove(key);
    }

    private void recordFailure(
            Map<String, FailureState> states,
            String key,
            int maxFailures,
            long windowMinutes,
            long lockMinutes) {
        Instant now = Instant.now();

        states.compute(key, (k, current) -> {
            if (current == null || current.windowStart.isBefore(now.minus(windowMinutes, ChronoUnit.MINUTES))) {
                current = new FailureState(now, 0, null);
            }
            if (current.lockedUntil != null && now.isBefore(current.lockedUntil)) {
                return current;
            }

            current.failureCount += 1;
            if (current.failureCount >= maxFailures) {
                current.lockedUntil = now.plus(lockMinutes, ChronoUnit.MINUTES);
            }
            return current;
        });
    }

    private String normalize(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? UNKNOWN : normalized;
    }

    private static class FailureState {
        private Instant windowStart;
        private int failureCount;
        private Instant lockedUntil;

        private FailureState(Instant windowStart, int failureCount, Instant lockedUntil) {
            this.windowStart = windowStart;
            this.failureCount = failureCount;
            this.lockedUntil = lockedUntil;
        }
    }

    private static class RequestState {
        private Instant windowStart;
        private int requestCount;

        private RequestState(Instant windowStart, int requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}
