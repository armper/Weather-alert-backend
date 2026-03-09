package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing a user in the system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String phoneNumber;
    private String name;
    private String passwordHash;
    private String role;
    private UserApprovalStatus approvalStatus;
    private Boolean emailVerified;
    private Boolean passwordResetRequired;
    private List<String> preferredNotificationChannels;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripePriceId;
    private String stripeSubscriptionStatus;
    private Instant stripeCurrentPeriodEnd;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
