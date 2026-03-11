package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.UserAccountResponse;
import com.weather.alert.application.usecase.ManageUserAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Admin Users", description = "Admin account management")
public class AdminUserApprovalController {

    private final ManageUserAccountUseCase manageUserAccountUseCase;

    @GetMapping
    @Operation(summary = "List all user accounts")
    public ResponseEntity<List<UserAccountResponse>> listAllAccounts() {
        return ResponseEntity.ok(manageUserAccountUseCase.listAllAccounts());
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend user account")
    public ResponseEntity<UserAccountResponse> suspendAccount(@PathVariable String userId) {
        return ResponseEntity.ok(manageUserAccountUseCase.suspendAccount(userId));
    }

    @PostMapping("/{userId}/reactivate")
    @Operation(summary = "Reactivate user account")
    public ResponseEntity<UserAccountResponse> reactivateAccount(@PathVariable String userId) {
        return ResponseEntity.ok(manageUserAccountUseCase.reactivateAccount(userId));
    }

    @PostMapping("/{userId}/force-password-reset")
    @Operation(summary = "Force password reset at next sign-in")
    public ResponseEntity<UserAccountResponse> forcePasswordReset(@PathVariable String userId) {
        return ResponseEntity.ok(manageUserAccountUseCase.forcePasswordReset(userId));
    }
}
