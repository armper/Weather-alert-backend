package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Request to resend registration email verification token",
        example = """
                {
                  "username": "alice"
                }
                """)
public class ResendRegistrationVerificationRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    @Schema(example = "alice")
    private String username;
}
