package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credentials used to obtain a JWT bearer token")
public class AuthRequest {
    @NotBlank
    @Schema(example = "dev-admin")
    private String username;

    @NotBlank
    @Schema(example = "dev-admin-password")
    private String password;
}
