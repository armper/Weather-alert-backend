package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request a one-time sign-in link by username or email")
public class MagicLinkRequest {

    @NotBlank
    @Schema(description = "Username or account email", example = "alice@example.com")
    private String usernameOrEmail;
}
