package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to confirm a channel verification token")
public class ConfirmChannelVerificationRequest {

    @NotBlank
    @Schema(description = "Raw verification token", example = "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5")
    private String token;
}
