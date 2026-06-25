package com.pixsom.racebets.entities;

import com.pixsom.racebets.entities.common.AuditableEntity;
import com.pixsom.racebets.enums.BetState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "bets")
public class Bet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name="race_entry_id", nullable = false)
    private RaceEntry raceEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private AppUser user;

    @Column(name="date_time_bet", nullable = false)
    private Instant dateTimeBet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BetState state;

    protected Bet() {
    }

    public Bet (RaceEntry raceEntry, AppUser user) {
        this.raceEntry = raceEntry;
        this.user = user;
        this.state = BetState.PENDING;
        this.dateTimeBet = Instant.now();
    }

    public void markAsWon() {
        state = BetState.WON;
    }

    public void markAsLost() {
        state = BetState.LOST;
    }
}
