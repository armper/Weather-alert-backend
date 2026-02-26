package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Update mutable account profile fields",
        example = """
                {
                  "name": "Alice B",
                  "phoneNumber": "+14075551234"
                }
                """)
public class UpdateMyAccountRequest {

    @Size(max = 255)
    @Schema(example = "Alice B")
    private String name;

    @Size(max = 32)
    @Schema(example = "+14075551234")
    private String phoneNumber;
}

