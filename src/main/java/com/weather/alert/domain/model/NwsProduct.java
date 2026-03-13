package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NwsProduct {
    private String id;
    private String wmoCollectiveId;
    private String issuingOffice;
    private Instant issuanceTime;
    private String productCode;
    private String productName;
    private String productText;
}
