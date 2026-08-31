package com.pixsom.racebets.auth.google;

import com.pixsom.racebets.entities.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "oauth_login_code")
public class OAuthLoginCode {

    @Id
    @Column(name = "code_hash", length = 64)
    private String codeHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "pending_issuer")
    private String pendingIssuer;

    @Column(name = "pending_subject")
    private String pendingSubject;

    @Column(name = "pending_email")
    private String pendingEmail;
}
