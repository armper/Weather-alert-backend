package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
        description = "Email verification confirmation for a registered user",
        example = """
                {
                  "userId": "alice",
                  "verificationId": "2b4f4f31-5a4c-45d8-b274-301f8c6fb5f4",
                  "token": "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5"
                }
                """)
public class VerifyRegistrationEmailRequest {

    @NotBlank
    @Schema(example = "alice")
    private String userId;

    @NotBlank
    @Schema(example = "2b4f4f31-5a4c-45d8-b274-301f8c6fb5f4")
    private String verificationId;

    @NotBlank
    @Schema(example = "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5")
    private String token;
}

