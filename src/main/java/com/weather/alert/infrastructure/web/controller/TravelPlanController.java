package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.TravelPlanRequest;
import com.weather.alert.application.dto.TravelPlanResponse;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.ManageTravelPlansUseCase;
import com.weather.alert.domain.model.TravelPlan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/travel-plans")
@RequiredArgsConstructor
@Tag(name = "Travel Plans", description = "Create and manage saved trips for weather monitoring")
public class TravelPlanController {

    private final ManageTravelPlansUseCase manageTravelPlansUseCase;

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "List travel plans for a user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Travel plans returned"),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<List<TravelPlanResponse>> getTravelPlansByUserId(
            @PathVariable String userId,
            Authentication authentication) {
        enforceUserAccess(authentication, userId);
        return ResponseEntity.ok(TravelPlanResponse.fromDomainList(manageTravelPlansUseCase.getByUserId(userId)));
    }

    @PostMapping
    @Operation(
            summary = "Create a travel plan",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Travel plan created"),
                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<TravelPlanResponse> createTravelPlan(
            @Valid @RequestBody TravelPlanRequest request,
            Authentication authentication) {
        request.setUserId(resolveOwnerUserIdForCreate(request.getUserId(), authentication));
        TravelPlan travelPlan = manageTravelPlansUseCase.create(request);
        return ResponseEntity.ok(TravelPlanResponse.fromDomain(travelPlan));
    }

    @PutMapping("/{travelPlanId}")
    @Operation(
            summary = "Update a travel plan",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Travel plan updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/problem+json")),
                    @ApiResponse(responseCode = "404", description = "Travel plan not found", content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<TravelPlanResponse> updateTravelPlan(
            @PathVariable String travelPlanId,
            @Valid @RequestBody TravelPlanRequest request,
            Authentication authentication) {
        TravelPlan existing = manageTravelPlansUseCase.getById(travelPlanId);
        enforceUserAccess(authentication, existing.getUserId());
        assertNonAdminNotActingAsAnotherUser(request.getUserId(), authentication);
        request.setUserId(existing.getUserId());
        TravelPlan travelPlan = manageTravelPlansUseCase.update(travelPlanId, request);
        return ResponseEntity.ok(TravelPlanResponse.fromDomain(travelPlan));
    }

    @DeleteMapping("/{travelPlanId}")
    @Operation(
            summary = "Delete a travel plan",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Travel plan deleted"),
                    @ApiResponse(responseCode = "404", description = "Travel plan not found", content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<Void> deleteTravelPlan(
            @PathVariable String travelPlanId,
            Authentication authentication) {
        TravelPlan existing = manageTravelPlansUseCase.getById(travelPlanId);
        enforceUserAccess(authentication, existing.getUserId());
        manageTravelPlansUseCase.delete(travelPlanId);
        return ResponseEntity.noContent().build();
    }

    private String resolveOwnerUserIdForCreate(String requestedUserId, Authentication authentication) {
        String authenticatedUserId = authenticatedUserId(authentication);
        if (isAdmin(authentication)) {
            if (requestedUserId == null || requestedUserId.isBlank()) {
                return authenticatedUserId;
            }
            return requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.isBlank() && !authenticatedUserId.equals(requestedUserId)) {
            throw new ForbiddenOperationException("Non-admin users can only manage their own travel plans");
        }
        return authenticatedUserId;
    }

    private void assertNonAdminNotActingAsAnotherUser(String requestedUserId, Authentication authentication) {
        if (isAdmin(authentication) || requestedUserId == null || requestedUserId.isBlank()) {
            return;
        }
        if (!authenticatedUserId(authentication).equals(requestedUserId)) {
            throw new ForbiddenOperationException("Non-admin users can only manage their own travel plans");
        }
    }

    private void enforceUserAccess(Authentication authentication, String resourceOwnerUserId) {
        if (isAdmin(authentication)) {
            return;
        }
        if (!authenticatedUserId(authentication).equals(resourceOwnerUserId)) {
            throw new ForbiddenOperationException("You do not have access to this travel plan");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ForbiddenOperationException("Unable to resolve authenticated user");
        }
        return authentication.getName();
    }
}
