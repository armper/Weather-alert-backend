package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Start password reset by username or email",
        example = """
                {
                  "usernameOrEmail": "alice@example.com"
                }
                """)
public class ForgotPasswordRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(example = "alice")
    private String usernameOrEmail;
}
