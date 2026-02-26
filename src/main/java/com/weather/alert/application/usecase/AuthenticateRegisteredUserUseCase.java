package com.weather.alert.application.usecase;

import com.weather.alert.application.exception.EmailVerificationRequiredException;
import com.weather.alert.application.exception.UserApprovalRequiredException;
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

@Service
@RequiredArgsConstructor
public class AuthenticateRegisteredUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<Authentication> authenticate(String username, String rawPassword) {
        User user = userRepository.findById(username).orElse(null);
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return Optional.empty();
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailVerificationRequiredException(username);
        }
        if (user.getApprovalStatus() != UserApprovalStatus.ACTIVE) {
            throw new UserApprovalRequiredException(username);
        }

        String role = user.getRole() == null || user.getRole().isBlank() ? "ROLE_USER" : user.getRole();
        return Optional.of(new UsernamePasswordAuthenticationToken(
                user.getId(),
                "n/a",
                List.of(new SimpleGrantedAuthority(role))));
    }
}

