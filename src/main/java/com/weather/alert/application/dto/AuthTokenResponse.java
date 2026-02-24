package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "JWT token response")
public class AuthTokenResponse {
    @Schema(
            description = "JWT access token. Use in Swagger Authorize dialog without the Bearer prefix",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZXYtYWRtaW4iLCJzY29wZSI6IlJPTEVfVVNFUiBST0xFX0FETUlOIiwiaXNzIjoid2VhdGhlci1hbGVydC1iYWNrZW5kIn0.signature")
    private String accessToken;

    @Schema(example = "Bearer")
    private String tokenType;

    @Schema(description = "Token TTL in seconds", example = "3600")
    private long expiresIn;
}
