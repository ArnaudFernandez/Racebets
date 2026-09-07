package com.pixsom.racebets.auth.google;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
    Optional<OAuthIdentity> findByIssuerAndSubject(String issuer, String subject);

    @Modifying
    @Query("delete from OAuthIdentity identity where identity.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
