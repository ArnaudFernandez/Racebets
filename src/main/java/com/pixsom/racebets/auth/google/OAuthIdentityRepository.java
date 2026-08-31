package com.pixsom.racebets.auth.google;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
    Optional<OAuthIdentity> findByIssuerAndSubject(String issuer, String subject);
}
