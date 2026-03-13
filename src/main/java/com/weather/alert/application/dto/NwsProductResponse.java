package com.weather.alert.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NWS text product (Area Forecast Discussion, Hazardous Weather Outlook, etc.)")
public class NwsProductResponse {
    @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(example = "FXUS66")
    private String wmoCollectiveId;

    @Schema(example = "SEW")
    private String issuingOffice;

    @Schema(example = "2026-03-13T00:00:00Z")
    private String issuanceTime;

    @Schema(example = "AFD")
    private String productCode;

    @Schema(example = "Area Forecast Discussion")
    private String productName;

    @Schema(description = "Full text of the NWS product")
    private String productText;
}
