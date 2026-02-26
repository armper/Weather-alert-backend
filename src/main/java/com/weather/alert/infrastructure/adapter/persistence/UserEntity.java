package com.weather.alert.infrastructure.adapter.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    
    @Id
    private String id;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "name")
    private String name;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "approval_status", nullable = false, length = 32)
    private String approvalStatus;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;
    
    @Column(name = "email_enabled")
    private Boolean emailEnabled;
    
    @Column(name = "sms_enabled")
    private Boolean smsEnabled;
    
    @Column(name = "push_enabled")
    private Boolean pushEnabled;

    @Column(name = "approved_at")
    private Instant approvedAt;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
