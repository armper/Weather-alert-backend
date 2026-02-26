package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
    
    private final JpaUserRepository jpaRepository;
    
    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<User> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }
    
    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
    
    private UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole() == null || user.getRole().isBlank() ? "ROLE_USER" : user.getRole())
                .approvalStatus((user.getApprovalStatus() == null ? UserApprovalStatus.ACTIVE : user.getApprovalStatus()).name())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .emailEnabled(user.getEmailEnabled())
                .smsEnabled(user.getSmsEnabled())
                .pushEnabled(user.getPushEnabled())
                .approvedAt(user.getApprovedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .name(entity.getName())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole() == null || entity.getRole().isBlank() ? "ROLE_USER" : entity.getRole())
                .approvalStatus(entity.getApprovalStatus() == null || entity.getApprovalStatus().isBlank()
                        ? UserApprovalStatus.ACTIVE
                        : UserApprovalStatus.valueOf(entity.getApprovalStatus()))
                .emailVerified(Boolean.TRUE.equals(entity.getEmailVerified()))
                .emailEnabled(entity.getEmailEnabled())
                .smsEnabled(entity.getSmsEnabled())
                .pushEnabled(entity.getPushEnabled())
                .approvedAt(entity.getApprovedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
