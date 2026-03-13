package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaProductResponse {
    private String id;
    private String wmoCollectiveId;
    private String issuingOffice;
    private String issuanceTime;
    private String productCode;
    private String productName;
    private String productText;
}
