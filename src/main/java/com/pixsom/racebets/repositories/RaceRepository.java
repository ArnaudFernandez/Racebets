package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;

public interface RaceRepository extends JpaRepository<Race, Long> {
    java.util.List<Race> findAllByState(com.pixsom.racebets.enums.RaceState state);
    boolean existsByStateInAndIdNot(Collection<com.pixsom.racebets.enums.RaceState> states, Long id);

    java.util.List<Race> findAllByVisibleOnLiveTrue(org.springframework.data.domain.Sort sort);

    java.util.List<Race> findAllByVisibleOnLiveTrueAndIdNot(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Race> findLockedById(Long id);
}
