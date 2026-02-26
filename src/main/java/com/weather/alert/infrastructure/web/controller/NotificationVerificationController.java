package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.ChannelVerificationResponse;
import com.weather.alert.application.dto.ConfirmChannelVerificationRequest;
import com.weather.alert.application.dto.StartChannelVerificationRequest;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.ManageChannelVerificationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/verifications")
@RequiredArgsConstructor
@Tag(name = "Notification Verification", description = "Start and confirm channel verification")
public class NotificationVerificationController {

    private final ManageChannelVerificationUseCase manageChannelVerificationUseCase;

    @PostMapping("/start")
    @Operation(
            summary = "Start notification channel verification",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "start-email-verification",
                                    value = """
                                            {
                                              "channel": "EMAIL",
                                              "destination": "dev-admin@example.com"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verification started"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation or unsupported channel",
                            content = @Content(mediaType = "application/problem+json")
                    )
            }
    )
    public ResponseEntity<ChannelVerificationResponse> startVerification(
            @Valid @RequestBody StartChannelVerificationRequest request,
            Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        ChannelVerificationResponse response = manageChannelVerificationUseCase.startVerification(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{verificationId}/confirm")
    @Operation(
            summary = "Confirm channel verification token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "confirm-token",
                                    value = """
                                            {
                                              "token": "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verification confirmed"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid or expired token",
                            content = @Content(mediaType = "application/problem+json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Verification id not found",
                            content = @Content(mediaType = "application/problem+json")
                    )
            }
    )
    public ResponseEntity<ChannelVerificationResponse> confirmVerification(
            @PathVariable String verificationId,
            @Valid @RequestBody ConfirmChannelVerificationRequest request,
            Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        ChannelVerificationResponse response =
                manageChannelVerificationUseCase.confirmVerification(userId, verificationId, request);
        return ResponseEntity.ok(response);
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ForbiddenOperationException("Unable to resolve authenticated user");
        }
        return authentication.getName();
    }
}
