package com.weather.alert.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for account recovery request endpoints")
public class RecoveryRequestResponse {

    @Schema(example = "If an account exists, recovery instructions were sent.")
    private String message;

    @Schema(example = "4f5f913d-baa8-4d20-8f72-e894712b8b23")
    private String recoveryId;

    @Schema(example = "2026-03-01T02:30:00Z")
    private Instant codeExpiresAt;

    @Schema(description = "Development-only raw recovery code (when enabled)", example = "A2B3C4D5")
    private String recoveryCode;

    @Schema(description = "Seconds until a new code request is allowed", example = "42")
    private Long retryAfterSeconds;
}
