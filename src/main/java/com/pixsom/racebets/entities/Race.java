package com.pixsom.racebets.entities;

import com.pixsom.racebets.entities.common.AuditableEntity;
import com.pixsom.racebets.enums.RaceState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "races")
public class Race extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String raceImgUrl;

    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable = false)
    private RaceState state;
}
