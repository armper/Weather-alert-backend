package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Self-service account registration request",
        example = """
                {
                  "username": "alice",
                  "password": "StrongPass123!",
                  "email": "alice@example.com",
                  "name": "Alice",
                  "phoneNumber": "+14075551234"
                }
                """)
public class RegisterUserRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    @Schema(example = "alice")
    private String username;

    @NotBlank
    @Size(min = 8, max = 128)
    @Schema(example = "StrongPass123!")
    private String password;

    @NotBlank
    @Email
    @Schema(example = "alice@example.com")
    private String email;

    @Size(max = 255)
    @Schema(example = "Alice")
    private String name;

    @Size(max = 32)
    @Schema(example = "+14075551234")
    private String phoneNumber;
}

