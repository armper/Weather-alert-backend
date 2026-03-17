package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.ConfirmPasswordResetRequest;
import com.weather.alert.application.dto.ConfirmUsernameRecoveryRequest;
import com.weather.alert.application.dto.ForgotPasswordRequest;
import com.weather.alert.application.dto.ForgotUsernameRequest;
import com.weather.alert.application.dto.MessageResponse;
import com.weather.alert.application.dto.RecoveryRequestResponse;
import com.weather.alert.application.dto.UsernameRecoveryResponse;
import com.weather.alert.application.usecase.ManageAccountRecoveryUseCase;
import com.weather.alert.infrastructure.web.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/recovery")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Account Recovery", description = "Forgot username and password reset flows")
public class AccountRecoveryController {

    private final ManageAccountRecoveryUseCase manageAccountRecoveryUseCase;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/username/request")
    @Operation(
            summary = "Request username recovery",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "forgot-username",
                                    value = """
                                            {
                                              "email": "alice@example.com"
                                            }
                                            """))),
            responses = @ApiResponse(responseCode = "200", description = "Recovery request accepted"))
    public ResponseEntity<RecoveryRequestResponse> requestUsernameRecovery(
            @Valid @RequestBody ForgotUsernameRequest request,
            HttpServletRequest servletRequest) {
        RecoveryRequestResponse response = manageAccountRecoveryUseCase.requestUsernameReminder(
                request,
                clientIp(servletRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/username/confirm")
    @Operation(
            summary = "Confirm username recovery code",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "confirm-username",
                                    value = """
                                            {
                                              "recoveryId": "4f5f913d-baa8-4d20-8f72-e894712b8b23",
                                              "code": "A2B3C4D5"
                                            }
                                            """))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Username recovered"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid or expired code",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<UsernameRecoveryResponse> confirmUsernameRecovery(
            @Valid @RequestBody ConfirmUsernameRecoveryRequest request,
            HttpServletRequest servletRequest) {
        UsernameRecoveryResponse response = manageAccountRecoveryUseCase.confirmUsernameReminder(
                request,
                clientIp(servletRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/request")
    @Operation(
            summary = "Request password reset",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "forgot-password",
                                    value = """
                                            {
                                              "usernameOrEmail": "alice@example.com"
                                            }
                                            """))),
            responses = @ApiResponse(responseCode = "200", description = "Recovery request accepted"))
    public ResponseEntity<RecoveryRequestResponse> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest) {
        RecoveryRequestResponse response = manageAccountRecoveryUseCase.requestPasswordReset(
                request,
                clientIp(servletRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/confirm")
    @Operation(
            summary = "Confirm password reset code",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "confirm-password-reset",
                                    value = """
                                            {
                                              "recoveryId": "4f5f913d-baa8-4d20-8f72-e894712b8b23",
                                              "code": "A2B3C4D5",
                                              "newPassword": "StrongPass123!"
                                            }
                                            """))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password reset successful"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid or expired code",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<MessageResponse> confirmPasswordReset(
            @Valid @RequestBody ConfirmPasswordResetRequest request,
            HttpServletRequest servletRequest) {
        MessageResponse response = manageAccountRecoveryUseCase.confirmPasswordReset(
                request,
                clientIp(servletRequest));
        return ResponseEntity.ok(response);
    }

    private String clientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
