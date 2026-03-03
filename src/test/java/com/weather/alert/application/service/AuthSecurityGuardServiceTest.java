package com.weather.alert.application.service;

import com.weather.alert.application.exception.TooManyRequestsException;
import com.weather.alert.domain.model.AccountRecoveryPurpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthSecurityGuardServiceTest {

    @Test
    void shouldLockLoginAfterConfiguredFailures() {
        AuthSecurityGuardService service = new AuthSecurityGuardService(
                2,
                15,
                15,
                10,
                15,
                3,
                15,
                15);

        assertDoesNotThrow(() -> service.assertLoginAllowed("alice", "127.0.0.1"));
        service.recordLoginFailure("alice", "127.0.0.1");
        service.recordLoginFailure("alice", "127.0.0.1");

        assertThrows(TooManyRequestsException.class, () -> service.assertLoginAllowed("alice", "127.0.0.1"));
    }

    @Test
    void shouldRateLimitRecoveryRequestsPerWindow() {
        AuthSecurityGuardService service = new AuthSecurityGuardService(
                6,
                15,
                15,
                2,
                15,
                6,
                15,
                15);

        assertDoesNotThrow(() -> service.consumeRecoveryRequestQuota(AccountRecoveryPurpose.PASSWORD_RESET, "alice", "127.0.0.1"));
        assertDoesNotThrow(() -> service.consumeRecoveryRequestQuota(AccountRecoveryPurpose.PASSWORD_RESET, "alice", "127.0.0.1"));

        assertThrows(TooManyRequestsException.class,
                () -> service.consumeRecoveryRequestQuota(AccountRecoveryPurpose.PASSWORD_RESET, "alice", "127.0.0.1"));
    }
}
