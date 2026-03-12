package com.weather.alert.application.usecase;

import com.weather.alert.application.exception.AccountSuspendedException;
import com.weather.alert.application.exception.EmailVerificationRequiredException;
import com.weather.alert.application.exception.PasswordResetRequiredException;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticateRegisteredUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<Authentication> authenticate(String usernameOrEmail, String rawPassword) {
        User user = resolveUser(usernameOrEmail).orElse(null);
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(toAuthentication(user));
    }

    public Optional<User> resolveUser(String usernameOrEmail) {
        String normalizedInput = normalize(usernameOrEmail);
        if (normalizedInput == null) {
            return Optional.empty();
        }

        Optional<User> byId = userRepository.findById(normalizedInput);
        if (byId.isPresent()) {
            return byId;
        }

        return userRepository.findByEmail(normalizedInput.toLowerCase(Locale.ROOT));
    }

    public Authentication toAuthentication(User user) {
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailVerificationRequiredException(user.getId());
        }
        if (user.getApprovalStatus() == UserApprovalStatus.SUSPENDED) {
            throw new AccountSuspendedException(user.getId());
        }
        if (Boolean.TRUE.equals(user.getPasswordResetRequired())) {
            throw new PasswordResetRequiredException(user.getId());
        }

        String role = user.getRole() == null || user.getRole().isBlank() ? "ROLE_USER" : user.getRole();
        return new UsernamePasswordAuthenticationToken(
                user.getId(),
                "n/a",
                List.of(new SimpleGrantedAuthority(role)));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
