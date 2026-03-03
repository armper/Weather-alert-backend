package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Confirm password reset code and set a new password",
        example = """
                {
                  "recoveryId": "4f5f913d-baa8-4d20-8f72-e894712b8b23",
                  "code": "A2B3C4D5",
                  "newPassword": "StrongPass123!"
                }
                """)
public class ConfirmPasswordResetRequest {

    @NotBlank
    private String recoveryId;

    @NotBlank
    private String code;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;
}
