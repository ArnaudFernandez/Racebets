package com.pixsom.racebets.entities;

import com.pixsom.racebets.entities.common.AuditableEntity;
import com.pixsom.racebets.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "app_user")
public class AppUser extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;
    private LocalDate birthDate;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String passwordHash;
    private boolean isPresent;

    @Column(nullable = false)
    private boolean tutorialCompleted;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<Role> roles;
}
