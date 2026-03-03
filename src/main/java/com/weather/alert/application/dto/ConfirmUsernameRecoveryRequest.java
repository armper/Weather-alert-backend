package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
        description = "Confirm username recovery code to reveal username",
        example = """
                {
                  "recoveryId": "4f5f913d-baa8-4d20-8f72-e894712b8b23",
                  "code": "A2B3C4D5"
                }
                """)
public class ConfirmUsernameRecoveryRequest {

    @NotBlank
    private String recoveryId;

    @NotBlank
    private String code;
}
