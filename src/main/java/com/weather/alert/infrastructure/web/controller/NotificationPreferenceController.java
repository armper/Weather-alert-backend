package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.CriteriaNotificationPreferenceResponse;
import com.weather.alert.application.dto.UpdateCriteriaNotificationPreferenceRequest;
import com.weather.alert.application.dto.UpdateUserNotificationPreferenceRequest;
import com.weather.alert.application.dto.UserNotificationPreferenceResponse;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.ManageNotificationPreferencesUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.UserNotificationPreference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "User defaults and criteria-level notification channel overrides")
public class NotificationPreferenceController {

    private final ManageNotificationPreferencesUseCase manageNotificationPreferencesUseCase;
    private final QueryAlertsUseCase queryAlertsUseCase;

    @GetMapping("/users/me/notification-preferences")
    @Operation(
            summary = "Get my notification preferences",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notification preferences returned"),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<UserNotificationPreferenceResponse> getMyNotificationPreferences(Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        UserNotificationPreference preference = manageNotificationPreferencesUseCase.getUserPreference(userId);
        return ResponseEntity.ok(UserNotificationPreferenceResponse.fromDomain(preference));
    }

    @PutMapping("/users/me/notification-preferences")
    @Operation(
            summary = "Update my notification preferences",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "user-default-preferences",
                                    value = """
                                            {
                                              "enabledChannels": ["EMAIL", "SMS"],
                                              "preferredChannel": "EMAIL",
                                              "fallbackStrategy": "FIRST_SUCCESS"
                                            }
                                            """))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notification preferences updated"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid configuration",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<UserNotificationPreferenceResponse> updateMyNotificationPreferences(
            @Valid @RequestBody UpdateUserNotificationPreferenceRequest request,
            Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        UserNotificationPreference saved = manageNotificationPreferencesUseCase.upsertUserPreference(userId, request);
        return ResponseEntity.ok(UserNotificationPreferenceResponse.fromDomain(saved));
    }

    @GetMapping("/criteria/{criteriaId}/notification-preferences")
    @Operation(
            summary = "Get criteria-level notification preference override",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Criteria notification preference returned"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json")),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Criteria not found",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<CriteriaNotificationPreferenceResponse> getCriteriaNotificationPreference(
            @PathVariable String criteriaId,
            Authentication authentication) {
        AlertCriteria criteria = queryAlertsUseCase.getCriteriaById(criteriaId);
        enforceUserAccess(authentication, criteria.getUserId());
        CriteriaNotificationPreference preference = manageNotificationPreferencesUseCase.getCriteriaPreference(criteriaId);
        return ResponseEntity.ok(CriteriaNotificationPreferenceResponse.fromDomain(preference));
    }

    @PutMapping("/criteria/{criteriaId}/notification-preferences")
    @Operation(
            summary = "Update criteria-level notification preference override",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "inherit-user-defaults",
                                            value = """
                                                    {
                                                      "useUserDefaults": true
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "explicit-criteria-override",
                                            value = """
                                                    {
                                                      "useUserDefaults": false,
                                                      "enabledChannels": ["EMAIL"],
                                                      "preferredChannel": "EMAIL",
                                                      "fallbackStrategy": "FIRST_SUCCESS"
                                                    }
                                                    """)
                            })),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Criteria notification preference updated"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid configuration",
                            content = @Content(mediaType = "application/problem+json")),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json")),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Criteria not found",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<CriteriaNotificationPreferenceResponse> updateCriteriaNotificationPreference(
            @PathVariable String criteriaId,
            @Valid @RequestBody UpdateCriteriaNotificationPreferenceRequest request,
            Authentication authentication) {
        AlertCriteria criteria = queryAlertsUseCase.getCriteriaById(criteriaId);
        enforceUserAccess(authentication, criteria.getUserId());
        CriteriaNotificationPreference saved =
                manageNotificationPreferencesUseCase.upsertCriteriaPreference(criteriaId, request);
        return ResponseEntity.ok(CriteriaNotificationPreferenceResponse.fromDomain(saved));
    }

    private void enforceUserAccess(Authentication authentication, String resourceOwnerUserId) {
        if (isAdmin(authentication)) {
            return;
        }
        String authenticatedUserId = authenticatedUserId(authentication);
        if (!authenticatedUserId.equals(resourceOwnerUserId)) {
            throw new ForbiddenOperationException("You do not have access to this criteria's notification preferences");
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

