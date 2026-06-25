package com.pixsom.racebets.entities;


import com.pixsom.racebets.entities.common.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "race_entries",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"race_id", "horse_id"}),
                @UniqueConstraint(columnNames = {"race_id", "horse_number"})
        }
)
public class RaceEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="horse_id", nullable = false)
    private Horse horse;


    @Column(name="horse_number", nullable = false)
    private int horseNumber;

    private Integer rank;
}
