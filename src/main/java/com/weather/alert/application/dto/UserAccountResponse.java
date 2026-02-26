package com.weather.alert.application.dto;

import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "User account profile and approval state")
public class UserAccountResponse {

    @Schema(example = "alice")
    private String id;

    @Schema(example = "alice@example.com")
    private String email;

    @Schema(example = "+14075551234", nullable = true)
    private String phoneNumber;

    @Schema(example = "Alice", nullable = true)
    private String name;

    @Schema(example = "ROLE_USER")
    private String role;

    @Schema(example = "PENDING_APPROVAL")
    private UserApprovalStatus approvalStatus;

    @Schema(example = "true")
    private Boolean emailVerified;

    @Schema(example = "2026-02-26T18:45:00Z", nullable = true)
    private Instant approvedAt;

    @Schema(example = "2026-02-26T18:40:00Z", nullable = true)
    private Instant createdAt;

    @Schema(example = "2026-02-26T18:41:30Z", nullable = true)
    private Instant updatedAt;

    public static UserAccountResponse fromDomain(User user) {
        return UserAccountResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .role(user.getRole())
                .approvalStatus(user.getApprovalStatus())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .approvedAt(user.getApprovedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

