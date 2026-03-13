package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.CreateTravelPlanRequest;
import com.weather.alert.application.dto.TravelPlanResponse;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.ManageTravelPlanUseCase;
import com.weather.alert.domain.model.TravelPlan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing travel plans.
 */
@RestController
@RequestMapping("/api/travel-plans")
@RequiredArgsConstructor
@Tag(name = "Travel Plans", description = "Create and manage user travel plans with weather alert monitoring")
public class TravelPlanController {

    private final ManageTravelPlanUseCase manageTravelPlanUseCase;

    @PostMapping
    @Operation(summary = "Create a travel plan", responses = {
            @ApiResponse(responseCode = "200", description = "Travel plan created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<TravelPlanResponse> createPlan(
            @Valid @RequestBody CreateTravelPlanRequest request,
            Authentication authentication) {
        String userId = resolveUserId(request.getUserId(), authentication);
        TravelPlan plan = manageTravelPlanUseCase.createPlan(userId, request);
        return ResponseEntity.ok(TravelPlanResponse.fromDomain(plan));
    }

    @PutMapping("/{planId}")
    @Operation(summary = "Update a travel plan", responses = {
            @ApiResponse(responseCode = "200", description = "Travel plan updated"),
            @ApiResponse(responseCode = "404", description = "Travel plan not found")
    })
    public ResponseEntity<TravelPlanResponse> updatePlan(
            @Parameter(example = "tp-abc123") @PathVariable String planId,
            @Valid @RequestBody CreateTravelPlanRequest request,
            Authentication authentication) {
        TravelPlan existing = manageTravelPlanUseCase.getPlanById(planId);
        enforceUserAccess(authentication, existing.getUserId());
        TravelPlan plan = manageTravelPlanUseCase.updatePlan(planId, request);
        return ResponseEntity.ok(TravelPlanResponse.fromDomain(plan));
    }

    @DeleteMapping("/{planId}")
    @Operation(summary = "Delete a travel plan", responses = {
            @ApiResponse(responseCode = "204", description = "Travel plan deleted"),
            @ApiResponse(responseCode = "404", description = "Travel plan not found")
    })
    public ResponseEntity<Void> deletePlan(
            @Parameter(example = "tp-abc123") @PathVariable String planId,
            Authentication authentication) {
        TravelPlan existing = manageTravelPlanUseCase.getPlanById(planId);
        enforceUserAccess(authentication, existing.getUserId());
        manageTravelPlanUseCase.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all travel plans for a user")
    public ResponseEntity<List<TravelPlanResponse>> getPlansForUser(
            @Parameter(example = "user-123") @PathVariable String userId,
            Authentication authentication) {
        enforceUserAccess(authentication, userId);
        List<TravelPlan> plans = manageTravelPlanUseCase.getPlansForUser(userId);
        return ResponseEntity.ok(TravelPlanResponse.fromDomainList(plans));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get a travel plan by ID")
    public ResponseEntity<TravelPlanResponse> getPlanById(
            @Parameter(example = "tp-abc123") @PathVariable String planId,
            Authentication authentication) {
        TravelPlan plan = manageTravelPlanUseCase.getPlanById(planId);
        enforceUserAccess(authentication, plan.getUserId());
        return ResponseEntity.ok(TravelPlanResponse.fromDomain(plan));
    }

    private String resolveUserId(String requestedUserId, Authentication authentication) {
        String authenticatedUserId = authenticatedUserId(authentication);
        if (isAdmin(authentication)) {
            return (requestedUserId == null || requestedUserId.isBlank()) ? authenticatedUserId : requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.isBlank() && !authenticatedUserId.equals(requestedUserId)) {
            throw new ForbiddenOperationException("Non-admin users can only manage their own travel plans");
        }
        return authenticatedUserId;
    }

    private void enforceUserAccess(Authentication authentication, String resourceOwnerUserId) {
        if (isAdmin(authentication)) {
            return;
        }
        String authenticatedUserId = authenticatedUserId(authentication);
        if (!authenticatedUserId.equals(resourceOwnerUserId)) {
            throw new ForbiddenOperationException("You do not have access to this travel plan");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ForbiddenOperationException("Unable to resolve authenticated user");
        }
        return authentication.getName();
    }
}
