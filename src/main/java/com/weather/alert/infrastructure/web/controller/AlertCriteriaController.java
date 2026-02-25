package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.AlertCriteriaQueryFilter;
import com.weather.alert.application.dto.AlertCriteriaResponse;
import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.ManageAlertCriteriaUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.AlertCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing alert criteria
 */
@RestController
@RequestMapping("/api/criteria")
@RequiredArgsConstructor
@Tag(name = "Alert Criteria", description = "Create and manage user alert criteria")
public class AlertCriteriaController {
    
    private final ManageAlertCriteriaUseCase manageAlertCriteriaUseCase;
    private final QueryAlertsUseCase queryAlertsUseCase;
    
    @PostMapping
    @Operation(
            summary = "Create alert criteria",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "orlando-temp-drop-and-rain",
                                    value = """
                                            {
                                              "userId": "dev-admin",
                                              "location": "Orlando",
                                              "latitude": 28.5383,
                                              "longitude": -81.3792,
                                              "temperatureThreshold": 60,
                                              "temperatureDirection": "BELOW",
                                              "temperatureUnit": "F",
                                              "rainThreshold": 40,
                                              "rainThresholdType": "PROBABILITY",
                                              "monitorCurrent": true,
                                              "monitorForecast": true,
                                              "forecastWindowHours": 48,
                                              "oncePerEvent": true,
                                              "rearmWindowMinutes": 120
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Criteria created"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(
                                    mediaType = "application/problem+json",
                                    examples = @ExampleObject(
                                            name = "validation-error",
                                            value = """
                                                    {
                                                      "type": "https://weather-alert-backend/errors/validation_error",
                                                      "title": "Bad Request",
                                                      "status": 400,
                                                      "detail": "Request validation failed",
                                                      "errorCode": "VALIDATION_ERROR",
                                                      "errors": [
                                                        {
                                                          "field": "request",
                                                          "message": "rainThreshold and rainThresholdType must be provided together"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<AlertCriteriaResponse> createCriteria(
            @Valid @RequestBody CreateAlertCriteriaRequest request,
            Authentication authentication) {
        request.setUserId(resolveOwnerUserIdForCreate(request.getUserId(), authentication));
        AlertCriteria criteria = manageAlertCriteriaUseCase.createCriteria(request);
        return ResponseEntity.ok(AlertCriteriaResponse.fromDomain(criteria));
    }
    
    @PutMapping("/{criteriaId}")
    @Operation(
            summary = "Update alert criteria",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "update-threshold-and-cooldown",
                                    value = """
                                            {
                                              "userId": "dev-admin",
                                              "location": "Orlando",
                                              "latitude": 28.5383,
                                              "longitude": -81.3792,
                                              "temperatureThreshold": 60,
                                              "temperatureDirection": "BELOW",
                                              "temperatureUnit": "F",
                                              "rainThreshold": 55,
                                              "rainThresholdType": "PROBABILITY",
                                              "monitorCurrent": true,
                                              "monitorForecast": true,
                                              "forecastWindowHours": 48,
                                              "oncePerEvent": true,
                                              "rearmWindowMinutes": 240
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Criteria updated"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(mediaType = "application/problem+json")
                    )
            }
    )
    public ResponseEntity<AlertCriteriaResponse> updateCriteria(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId,
            @Valid @RequestBody CreateAlertCriteriaRequest request,
            Authentication authentication) {
        AlertCriteria existing = queryAlertsUseCase.getCriteriaById(criteriaId);
        enforceUserAccess(authentication, existing.getUserId());
        assertNonAdminNotActingAsAnotherUser(request.getUserId(), authentication);
        request.setUserId(existing.getUserId());
        AlertCriteria criteria = manageAlertCriteriaUseCase.updateCriteria(criteriaId, request);
        return ResponseEntity.ok(AlertCriteriaResponse.fromDomain(criteria));
    }
    
    @DeleteMapping("/{criteriaId}")
    @Operation(
            summary = "Delete alert criteria",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Criteria deleted"),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Criteria not found",
                            content = @Content(mediaType = "application/problem+json")
                    )
            }
    )
    public ResponseEntity<Void> deleteCriteria(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId,
            Authentication authentication) {
        AlertCriteria existing = queryAlertsUseCase.getCriteriaById(criteriaId);
        enforceUserAccess(authentication, existing.getUserId());
        manageAlertCriteriaUseCase.deleteCriteria(criteriaId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get criteria for a user with optional filters")
    public ResponseEntity<List<AlertCriteriaResponse>> getCriteriaByUserId(
            @Parameter(example = "user-123") @PathVariable String userId,
            @Parameter(description = "Filter by temperature unit preference", example = "F")
            @RequestParam(required = false) AlertCriteria.TemperatureUnit temperatureUnit,
            @Parameter(description = "Filter by monitorCurrent flag", example = "true")
            @RequestParam(required = false) Boolean monitorCurrent,
            @Parameter(description = "Filter by monitorForecast flag", example = "true")
            @RequestParam(required = false) Boolean monitorForecast,
            @Parameter(description = "Filter by enabled flag", example = "true")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Only include criteria that do (or do not) define a temperature rule", example = "true")
            @RequestParam(required = false) Boolean hasTemperatureRule,
            @Parameter(description = "Only include criteria that do (or do not) define a rain rule", example = "true")
            @RequestParam(required = false) Boolean hasRainRule,
            Authentication authentication) {
        enforceUserAccess(authentication, userId);
        AlertCriteriaQueryFilter filter = AlertCriteriaQueryFilter.builder()
                .temperatureUnit(temperatureUnit)
                .monitorCurrent(monitorCurrent)
                .monitorForecast(monitorForecast)
                .enabled(enabled)
                .hasTemperatureRule(hasTemperatureRule)
                .hasRainRule(hasRainRule)
                .build();
        List<AlertCriteria> criteria = queryAlertsUseCase.getCriteriaByUserId(userId, filter);
        return ResponseEntity.ok(AlertCriteriaResponse.fromDomainList(criteria));
    }
    
    @GetMapping("/{criteriaId}")
    @Operation(summary = "Get criteria by ID")
    public ResponseEntity<AlertCriteriaResponse> getCriteriaById(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId,
            Authentication authentication) {
        AlertCriteria criteria = queryAlertsUseCase.getCriteriaById(criteriaId);
        enforceUserAccess(authentication, criteria.getUserId());
        return ResponseEntity.ok(AlertCriteriaResponse.fromDomain(criteria));
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
            throw new ForbiddenOperationException("Non-admin users can only manage their own alert criteria");
        }
        return authenticatedUserId;
    }

    private void assertNonAdminNotActingAsAnotherUser(String requestedUserId, Authentication authentication) {
        if (isAdmin(authentication) || requestedUserId == null || requestedUserId.isBlank()) {
            return;
        }
        String authenticatedUserId = authenticatedUserId(authentication);
        if (!authenticatedUserId.equals(requestedUserId)) {
            throw new ForbiddenOperationException("Non-admin users can only manage their own alert criteria");
        }
    }

    private void enforceUserAccess(Authentication authentication, String resourceOwnerUserId) {
        if (isAdmin(authentication)) {
            return;
        }
        String authenticatedUserId = authenticatedUserId(authentication);
        if (!authenticatedUserId.equals(resourceOwnerUserId)) {
            throw new ForbiddenOperationException("You do not have access to this user's alert criteria");
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
