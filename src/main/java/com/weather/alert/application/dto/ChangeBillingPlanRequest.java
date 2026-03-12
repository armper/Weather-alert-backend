package com.weather.alert.application.dto;

import com.weather.alert.domain.model.BillingPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeBillingPlanRequest {
    private BillingPlan plan;
}
