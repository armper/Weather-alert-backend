package com.weather.alert.domain.port;

import com.weather.alert.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Port for user persistence
 */
public interface UserRepositoryPort {
    
    User save(User user);
    
    Optional<User> findById(String id);
    
    Optional<User> findByEmail(String email);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    Optional<User> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    List<User> findAll();
    
    void delete(String id);
}
