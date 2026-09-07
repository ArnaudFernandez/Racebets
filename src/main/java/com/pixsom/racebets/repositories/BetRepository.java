package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.Bet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BetRepository extends JpaRepository<Bet, Long> {

    @EntityGraph(attributePaths = {"raceEntry", "raceEntry.race", "raceEntry.horse"})
    Optional<Bet> findByUser_IdAndRaceEntry_Race_Id(Long userId, Long raceId);

    @EntityGraph(attributePaths = {"user", "raceEntry", "raceEntry.race", "raceEntry.horse"})
    List<Bet> findAllByRaceEntry_Race_IdOrderByDateTimeBetAscIdAsc(Long raceId);

    @EntityGraph(attributePaths = {"user", "raceEntry", "raceEntry.race", "raceEntry.horse"})
    List<Bet> findAllByRaceEntry_Race_StateOrderByDateTimeBetAscIdAsc(com.pixsom.racebets.enums.RaceState state);

    @EntityGraph(attributePaths = {"raceEntry", "raceEntry.race", "raceEntry.horse"})
    List<Bet> findAllByUser_IdAndRaceEntry_Race_State(Long userId, com.pixsom.racebets.enums.RaceState state);

    boolean existsByUser_IdAndRaceEntry_Race_State(Long userId, com.pixsom.racebets.enums.RaceState state);

    @Modifying
    @Query("delete from Bet bet where bet.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"raceEntry", "raceEntry.race", "raceEntry.horse"})
    List<Bet> findAllByRaceEntry_Race_IdInAndStateOrderByDateTimeBetAscIdAsc(
            List<Long> raceIds, com.pixsom.racebets.enums.BetState state);

    @Query("""
            select b.raceEntry.id as raceEntryId, count(b) as betCount
            from Bet b
            where b.raceEntry.race.id = :raceId
            group by b.raceEntry.id
            """)
    List<BetCountView> countByRaceEntryForRace(Long raceId);

    interface BetCountView {
        Long getRaceEntryId();
        long getBetCount();
    }
}
