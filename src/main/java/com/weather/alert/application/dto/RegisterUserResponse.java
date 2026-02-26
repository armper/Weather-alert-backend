package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Registration result with verification step details")
public class RegisterUserResponse {

    @Schema
    private UserAccountResponse account;

    @Schema
    private ChannelVerificationResponse emailVerification;
}

