package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
        description = "Start username recovery by email",
        example = """
                {
                  "email": "alice@example.com"
                }
                """)
public class ForgotUsernameRequest {

    @NotBlank
    @Email
    @Schema(example = "alice@example.com")
    private String email;
}
