package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.AuthRequest;
import com.weather.alert.application.dto.AuthTokenResponse;
import com.weather.alert.application.dto.ConfirmMagicLinkRequest;
import com.weather.alert.application.dto.MagicLinkRequest;
import com.weather.alert.application.dto.RecoveryRequestResponse;
import com.weather.alert.application.exception.InvalidCredentialsException;
import com.weather.alert.application.service.AuthSecurityGuardService;
import com.weather.alert.application.usecase.AuthenticateRegisteredUserUseCase;
import com.weather.alert.application.usecase.ManageAccountRecoveryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Authentication", description = "JWT token issuance")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final AuthenticateRegisteredUserUseCase authenticateRegisteredUserUseCase;
    private final ManageAccountRecoveryUseCase manageAccountRecoveryUseCase;
    private final AuthSecurityGuardService authSecurityGuardService;
    private final JwtEncoder jwtEncoder;

    @Value("${app.security.jwt.expiration-seconds:3600}")
    private long jwtExpirationSeconds;

    @PostMapping("/token")
    @Operation(
            summary = "Issue JWT token",
            description = "Authenticate with configured local credentials and return a bearer JWT for protected endpoints.")
    public ResponseEntity<AuthTokenResponse> generateToken(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = clientIp(servletRequest);
        authSecurityGuardService.assertLoginAllowed(request.getUsername(), clientIp);

        Authentication authentication;
        try {
            authentication = authenticate(request);
        } catch (InvalidCredentialsException ex) {
            authSecurityGuardService.recordLoginFailure(request.getUsername(), clientIp);
            log.warn("AUTH_LOGIN_FAILURE username={} ip={}", request.getUsername(), clientIp);
            throw ex;
        }
        authSecurityGuardService.clearLoginFailures(request.getUsername(), clientIp);
        log.info("AUTH_LOGIN_SUCCESS username={} ip={}", authentication.getName(), clientIp);

        return ResponseEntity.ok(issueToken(authentication));
    }

    @PostMapping("/magic-link/request")
    @Operation(summary = "Request a one-time sign-in link")
    public ResponseEntity<RecoveryRequestResponse> requestMagicLink(
            @Valid @RequestBody MagicLinkRequest request,
            HttpServletRequest servletRequest) {
        RecoveryRequestResponse response = manageAccountRecoveryUseCase.requestMagicLogin(
                request,
                clientIp(servletRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/magic-link/confirm")
    @Operation(summary = "Confirm a one-time sign-in link and issue a JWT")
    public ResponseEntity<AuthTokenResponse> confirmMagicLink(
            @Valid @RequestBody ConfirmMagicLinkRequest request,
            HttpServletRequest servletRequest) {
        Authentication authentication = manageAccountRecoveryUseCase.confirmMagicLogin(
                request,
                clientIp(servletRequest));
        return ResponseEntity.ok(issueToken(authentication));
    }

    private AuthTokenResponse issueToken(Authentication authentication) {
        Instant now = Instant.now();
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("weather-alert-backend")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtExpirationSeconds))
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();

        return AuthTokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationSeconds)
                .build();
    }

    private Authentication authenticate(AuthRequest request) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ignored) {
            return authenticateRegisteredUserUseCase
                    .authenticate(request.getUsername(), request.getPassword())
                    .orElseThrow(InvalidCredentialsException::new);
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null || request.getRemoteAddr() == null || request.getRemoteAddr().isBlank()) {
            return "unknown";
        }
        return request.getRemoteAddr();
    }
}
