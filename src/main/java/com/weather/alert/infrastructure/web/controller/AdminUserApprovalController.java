package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.UserAccountResponse;
import com.weather.alert.application.usecase.ManageUserAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin review and approval of registered accounts")
public class AdminUserApprovalController {

    private final ManageUserAccountUseCase manageUserAccountUseCase;

    @GetMapping("/pending")
    @Operation(
            summary = "List pending user accounts",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pending accounts list"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<List<UserAccountResponse>> listPendingAccounts() {
        return ResponseEntity.ok(manageUserAccountUseCase.listPendingAccounts());
    }

    @PostMapping("/{userId}/approve")
    @Operation(
            summary = "Approve a pending user account",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account approved"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid approval state",
                            content = @Content(mediaType = "application/problem+json")),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<UserAccountResponse> approveAccount(@PathVariable String userId) {
        return ResponseEntity.ok(manageUserAccountUseCase.approveAccount(userId));
    }
}

