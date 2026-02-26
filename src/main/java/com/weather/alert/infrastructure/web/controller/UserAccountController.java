package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.UpdateMyAccountRequest;
import com.weather.alert.application.dto.UserAccountResponse;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.ManageUserAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "Manage own account profile")
public class UserAccountController {

    private final ManageUserAccountUseCase manageUserAccountUseCase;

    @GetMapping
    @Operation(
            summary = "Get my account",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account details"),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<UserAccountResponse> getMyAccount(Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        return ResponseEntity.ok(manageUserAccountUseCase.getMyAccount(userId));
    }

    @PutMapping
    @Operation(
            summary = "Update my account profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account updated"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<UserAccountResponse> updateMyAccount(
            @Valid @RequestBody UpdateMyAccountRequest request,
            Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        return ResponseEntity.ok(manageUserAccountUseCase.updateMyAccount(userId, request));
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ForbiddenOperationException("Unable to resolve authenticated user");
        }
        return authentication.getName();
    }
}

