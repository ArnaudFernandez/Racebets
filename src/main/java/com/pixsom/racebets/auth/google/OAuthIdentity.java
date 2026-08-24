package com.pixsom.racebets.auth.google;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.entities.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "oauth_identity", uniqueConstraints =
        @UniqueConstraint(name = "uk_oauth_identity_issuer_subject", columnNames = {"issuer", "subject"}))
public class OAuthIdentity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String issuer;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "email_at_link", nullable = false)
    private String emailAtLink;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
}
