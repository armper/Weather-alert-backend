package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Change password for currently authenticated user",
        example = """
                {
                  "currentPassword": "OldPass123!",
                  "newPassword": "NewStrongPass123!",
                  "confirmNewPassword": "NewStrongPass123!"
                }
                """)
public class ChangePasswordRequest {

    @NotBlank
    @Size(min = 8, max = 128)
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;

    @NotBlank
    @Size(min = 8, max = 128)
    private String confirmNewPassword;

    @AssertTrue(message = "newPassword and confirmNewPassword must match")
    public boolean isPasswordConfirmationValid() {
        if (newPassword == null || confirmNewPassword == null) {
            return true;
        }
        return newPassword.equals(confirmNewPassword);
    }
}
