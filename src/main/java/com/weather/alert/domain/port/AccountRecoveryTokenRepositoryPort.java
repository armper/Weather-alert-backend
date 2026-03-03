package com.weather.alert.domain.port;

import com.weather.alert.domain.model.AccountRecoveryToken;
import com.weather.alert.domain.model.AccountRecoveryPurpose;

import java.util.Optional;

public interface AccountRecoveryTokenRepositoryPort {

    AccountRecoveryToken save(AccountRecoveryToken token);

    Optional<AccountRecoveryToken> findById(String id);

    Optional<AccountRecoveryToken> findLatestByUserIdAndPurpose(String userId, AccountRecoveryPurpose purpose);
}
