package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.RaceEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceEntryRepository extends JpaRepository<RaceEntry, Long> {

    @EntityGraph(attributePaths = {"race", "horse"})
    List<RaceEntry> findAllBy(Sort sort);

    List<RaceEntry> findAllByRace_Id(Long raceId, Sort sort);

    boolean existsByRace_IdAndHorse_Id(Long raceId, Long horseId);

    boolean existsByRace_IdAndHorse_IdAndIdNot(Long raceId, Long horseId, Long id);

    boolean existsByRace_IdAndHorseNumber(Long raceId, int horseNumber);

    boolean existsByRace_IdAndHorseNumberAndIdNot(Long raceId, int horseNumber, Long id);
}
