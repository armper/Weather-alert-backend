package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.RegisterUserRequest;
import com.weather.alert.application.dto.RegisterUserResponse;
import com.weather.alert.application.dto.ResendRegistrationVerificationRequest;
import com.weather.alert.application.dto.UserAccountResponse;
import com.weather.alert.application.dto.VerifyRegistrationEmailRequest;
import com.weather.alert.application.dto.ChannelVerificationResponse;
import com.weather.alert.application.usecase.ManageUserAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Registration", description = "User registration, email verification, and account onboarding")
public class RegistrationController {

    private final ManageUserAccountUseCase manageUserAccountUseCase;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new account",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "register-user",
                                    value = """
                                            {
                                              "username": "alice",
                                              "password": "StrongPass123!",
                                              "email": "alice@example.com",
                                              "name": "Alice",
                                              "phoneNumber": "+14075551234"
                                            }
                                            """))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account registered (pending approval)"),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Username/email conflict",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisterUserResponse response = manageUserAccountUseCase.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/verify-email")
    @Operation(
            summary = "Verify registration email",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "verify-email",
                                    value = """
                                            {
                                              "userId": "alice",
                                              "verificationId": "2b4f4f31-5a4c-45d8-b274-301f8c6fb5f4",
                                              "token": "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5"
                                            }
                                            """))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Email verified"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid/expired token",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<UserAccountResponse> verifyRegistrationEmail(
            @Valid @RequestBody VerifyRegistrationEmailRequest request) {
        UserAccountResponse response = manageUserAccountUseCase.verifyRegisteredEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/resend-verification")
    @Operation(
            summary = "Resend registration email verification token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "resend-verification",
                                    value = """
                                            {
                                              "username": "alice"
                                            }
                                            """))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verification token reissued"),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<ChannelVerificationResponse> resendVerification(
            @Valid @RequestBody ResendRegistrationVerificationRequest request) {
        ChannelVerificationResponse response = manageUserAccountUseCase.resendRegistrationEmailVerification(request);
        return ResponseEntity.ok(response);
    }
}
