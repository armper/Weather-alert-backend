package com.weather.alert.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthTokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
