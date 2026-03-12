package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Confirm a one-time sign-in link or code")
public class ConfirmMagicLinkRequest {

    @NotBlank
    @Schema(example = "4f5f913d-baa8-4d20-8f72-e894712b8b23")
    private String recoveryId;

    @NotBlank
    @Schema(example = "A2B3C4D5")
    private String code;
}
