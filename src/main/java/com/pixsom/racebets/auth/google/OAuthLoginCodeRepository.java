package com.pixsom.racebets.auth.google;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from OAuthLoginCode code join fetch code.user where code.codeHash = :codeHash")
    Optional<OAuthLoginCode> findLockedByCodeHash(String codeHash);

    @Modifying
    @Query("delete from OAuthLoginCode code where code.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    void deleteByExpiresAtBefore(Instant threshold);
}
