package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.CreateBillingCheckoutSessionUseCase;
import com.weather.alert.application.usecase.GetBillingStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Stripe billing and subscription endpoints")
public class BillingController {

    private final GetBillingStatusUseCase getBillingStatusUseCase;
    private final CreateBillingCheckoutSessionUseCase createBillingCheckoutSessionUseCase;

    @GetMapping("/me")
    @Operation(
            summary = "Get my billing status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Billing status"),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<BillingStatusResponse> getMyBillingStatus(Authentication authentication) {
        return ResponseEntity.ok(getBillingStatusUseCase.getForUser(authenticatedUserId(authentication)));
    }

    @PostMapping("/checkout-session")
    @Operation(
            summary = "Create a Stripe Checkout session for a subscription",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Checkout session created"),
                    @ApiResponse(
                            responseCode = "409",
                            description = "User already has an active subscription",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<BillingCheckoutSessionResponse> createCheckoutSession(Authentication authentication) {
        return ResponseEntity.ok(createBillingCheckoutSessionUseCase.createForUser(authenticatedUserId(authentication)));
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ForbiddenOperationException("Unable to resolve authenticated user");
        }
        return authentication.getName();
    }
}
