package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned after successful username recovery confirmation")
public class UsernameRecoveryResponse {

    @Schema(example = "Username recovered successfully.")
    private String message;

    @Schema(example = "alice")
    private String username;
}
